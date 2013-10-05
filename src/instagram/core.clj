(ns instagram.core
  (require [clj-http.client :as client]
           [clojure.data.json :as json]
           [clojure.set]))

(def instagram-client-id "e964033fc4f7456092d43555a94fb972")

(defn get-rel-url
  "get url for `follows` or `followed-by` relationship"
  [user-id relationship]
  (str "https://api.instagram.com/v1/users/"
       user-id
       "/"
       relationship
       "?client_id="
       instagram-client-id))

(defn parsed-body
  "parsed json response"
  [response]
  (json/read-str (:body response)))

(defn get-next-page [parsed-body]
  "get next page url from response"
  (get-in parsed-body ["pagination" "next_url"]))

(defn users-list [parsed-body]
  "get parsed hash-map of users from response"
  (get parsed-body "data"))


(defn get-users-list
  "get `follows` or `followed-by` users for given user-id"
  [user-id relationship]
  (loop [list []
         next-url (get-rel-url user-id relationship)]
      (if next-url
        (let [body (parsed-body (client/get next-url))]
          (recur (concat list (users-list body)) (get-next-page body)))
        list)))

(defn get-id-set
  "converts users list from jnos response to a set of integer ids"
  [users-list]
  (set (map #(get % "id") users-list)))

(defn get-name-by-id
  [user-list id]
  (some #(if (= id (get % "id")) (get % "username")) user-list))

(defn -main
  [user-id]
  (let [follows (future (get-users-list user-id "follows"))
        followed-by (future (get-users-list user-id "followed-by"))
        follows-ids (get-id-set @follows)
        followed-by-ids (get-id-set @followed-by)
        difference (clojure.set/difference follows-ids followed-by-ids)]
    (println (clojure.string/join "\n"
                (map (partial get-name-by-id @follows) difference)))
    (System/exit 0)))
