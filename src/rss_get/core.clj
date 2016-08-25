(ns rss-get.core
    (:require [feedparser-clj.core :as feed]
              [tblibrary.updater :as updater]
              [tblibrary.bot :as bot]
              [tblibrary.emoji :as emoji]
              [tblibrary.handlers :as handlers]
              [tblibrary.filters :as filters]
              [clojure.core.async :as async]
              [clojure.string :as string]
              [clojure.tools.logging :as log]
              [clojure.data.json :as json])
    (:gen-class))

;; Telegram bot token
(def bot-token "***REMOVED***")

;; Feeds file
(def feeds-file "./feeds.json")

;; Now function
(defn now [] (new java.util.Date))

;; Last update
;; TODO Make it better
(def last-update (atom (now)))
(defn get-last-update []
    @last-update)
(defn set-last-update 
    ([]
        (set-last-update (now)))
    ([n]
        (swap! last-update (fn [_] n))))

(defn read-feeds [file]
    (if (.exists (clojure.java.io/as-file file))
        (into {} 
          (map (fn [[feed users]] 
            [feed (into {} 
              (map (fn [[id name]] 
                [(read-string id) name]) users))]) 
          (json/read-str (slurp file))))
        {}))

(defn write-feeds [feeds file]
    (with-open [wrtr (clojure.java.io/writer file)]
          (.write wrtr (json/write-str feeds)))
    feeds)

;; Users
(def feeds (atom (read-feeds feeds-file)))
(defn get-feeds []
    @feeds)
(defn add-user [id username feed]
    (swap! feeds update-in [feed] assoc id username))
(defn remove-user [id username feed]
    (swap! feeds update-in [feed] dissoc id))

(def main-flds [:authors :categories :contributors :copyright :description :encoding :entries :feed-type :image :language :link :entry-links :published-date :title :uri])
(def entries-flds [:authors :categories :contents :contributors :description :enclosures :link :published-date :title :updated-date :url :uri])

(defn html-escape [s]
    (string/escape s {\< "&lt;", \> "&gt;", \& "&amp;"}))

(defn entry-date [entry]
    (or (:published-date entry) (:updated-date entry) (now)))

(defn read-rss [url last-update]
    (log/info "Read feeds from" url)
    (let [f (feed/parse-feed url)
          result (update-in f [:entries] (fn [ent] (doall (filter #(.after (entry-date %) last-update) ent))))]
        (log/info "New feeds count:" (count (:entries result)))
        result))

(defn render-entry [entry]
    (let [t (html-escape (:title entry))
          ;d (html-escape (:value (:description entry)))
          u (:link entry)]
        (str "<b>" t "</b>\n" u)))

(defn rss-distribution [users rss]
    (log/info users)
    (log/info "Send feeds to users" (map #(first %) users))
    (doseq [user users
            ent (:entries rss)]
            (bot/send_message bot-token (first user) (render-entry ent) nil "HTML")))

;; TODO
(defn validate-feed [feed]
    true)

(defn add-track-user [req]
    (let [id (get-in req [:message :chat :id])
          username (get-in req [:message :chat :username])
          params (rest (string/split (get-in req [:message :text]) #" "))]
        (if (not= (count params) 1)
            (bot/send_message bot-token id "Please, provide a feed URL to which you want to subscribe. For example, /subscribe https://blog.superfeedr.com/atom.xml")
            (let [feed (first params)]
                (if (not (validate-feed feed))
                    (bot/send_message bot-token id "We could not subscribe you to this feed... sorry!")
                    (do
                        (log/info "User" id "subscribed to feed" feed)
                        (write-feeds (add-user id username feed) feeds-file)
                        (bot/send_message bot-token id "Done! Next time the feed updates, you'll be the first to know!"))))))
    (log/info (get-feeds)))

(defn remove-track-user [req]
    (let [id (get-in req [:message :chat :id])
          username (get-in req [:message :chat :username])
          params (rest (string/split (get-in req [:message :text]) #" "))]
        (if (not= (count params) 1)
            (bot/send_message bot-token id "Please, provide a feed URL to which you want to unsubscribe. For example, /unsubscribe https://blog.superfeedr.com/atom.xml")
            (let [feed (first params)
                  feeds (get-feeds)]
                (if (not (contains? feeds feed))
                    (bot/send_message bot-token id "We could not unsubscribe you to this feed... sorry!")
                    (do 
                        (log/info "User" id "unsubscribed from feed" feed)
                        (write-feeds (remove-user id username feed) feeds-file)
                        (bot/send_message bot-token id (str "Done! You will not hear from this feed again " emoji/UNAMUSED_FACE)))))))
    (log/info (get-feeds)))

(defn feeds-list [req]
    (let [id (get-in req [:message :chat :id])
          feeds (map #(first %) (filter (fn [[feed users]] (some #(= (first %) id) users)) (get-feeds)))]
          (if (= (count feeds) 0)
              (bot/send_message bot-token id "Yout dont recieve any notifications from me!")
              (bot/send_message bot-token id (string/join "\n" feeds)))
        feeds))

(defn clear-list [req]
    (let [id (get-in req [:message :chat :id])
          username (get-in req [:message :chat :username])
          feeds (map #(first %) (filter (fn [[feed users]] (some #(= (first %) id) users)) (get-feeds)))]
        (doseq [feed feeds]
            (remove-user id username feed))
        (write-feeds (get-feeds) feeds-file)
        (bot/send_message bot-token id (str "Done! You will not hear any word from me again " emoji/UNAMUSED_FACE))))

(defn start-polling []
    (async/go-loop []
        (log/info "Loop iteration...")
        (let [feeds (get-feeds)
              last-update (get-last-update)]
            (doseq [rss-url (keys feeds)
                    :when (> (count (get feeds rss-url)) 0)]
                (async/go (rss-distribution (get feeds rss-url) (read-rss rss-url last-update)))))

        (set-last-update)
        (async/<! (async/timeout (* 1000 5 60)))   ;; pause 5 min
        (recur)))

(def h 
    [(handlers/create_command "subscribe" add-track-user)
     (handlers/create_command "unsubscribe" remove-track-user)
     (handlers/create_command "list" feeds-list)
     (handlers/create_command "clear" clear-list)
     (handlers/create_command "help" #(bot/send_message bot-token (get-in % [:message :chat :id]) "HELP!"))
     (handlers/create_command "start" #(bot/send_message bot-token (get-in % [:message :chat :id]) "Hi! I am feeder bot for telegram! Please, use /help to get the list of commands"))
     (handlers/create_handler filters/text #(bot/send_message bot-token (get-in % [:message :chat :id]) "Please, use /help to get the list of commands"))])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (log/info (get-last-update))
  ;; Start Bot
  ;;(updater/start_handlers h (updater/start_polling bot-token 100 1000 0))
  (updater/start_handlers h (updater/start_webhook bot-token "tzapil.tk" 8443 "hook" "cert.pem" "cert.keystore" "LjvbrGfhbn1"))
  ;; Start poll rss
  (start-polling)
  ;; idle
  (updater/idle))
