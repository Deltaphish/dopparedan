(ns dfd
  (:require [clojure.string :as str]
            [java-time.api :as t]))

(defn generate-text [count]
  (str (str/join (repeat count "dan före "))
       "dopparedan!"))


(defn render-attr
  [attr]
  (if (map? attr)
    (str/join " " (map
                   (fn [key] (str (name key) "=\"" (key attr) "\""))
                   (keys attr)))
    ""))

(defmulti render (fn [elem] (:type elem)))
(defmethod render :default [_elem] "")

(defn tag? [t]
  (:type t))


(defmacro html-tag
  [tag-name]
  (list (list 'defn (symbol tag-name)
              (list '[& children] {:type (keyword tag-name)
                                   :children '(filter tag? children)
                                   :attr '(first (filter (fn [x] (not (tag? x))) children))}))
        (list 'defn (symbol (str (name tag-name) "?")) '[elem]
              (list '= (keyword tag-name) '(:type elem)))
        (list 'defmethod 'render (keyword tag-name) '[elem]
              (list 'format
                    (list 'str (format "<%s " (name tag-name))
                          '(render-attr (:attr elem))
                          ">%s"
                          (format "</%s>" (name tag-name)))
                    '(str/join "" (map render (:children elem)))))))

(html-tag "div")
(html-tag "html")
(html-tag "head")
(html-tag "body")


(defn p
  ([content] {:type :p :text content})
  ([attr content] {:type :p :text content :attr attr}))

(defn h1
  ([content] {:type :h1 :text content})
  ([attr content] {:type :h1 :text content :attr attr}))

(defmethod render :h1 [elem] (format "<h1 %s>%s</h1>" (render-attr (:attr elem)) (:text elem)))

(defn title
  ([content] {:type :title :text content})
  ([attr content] {:type :title :text content :attr attr}))

(defmethod render :title [elem] (format "<title %s>%s</title>" (render-attr (:attr elem)) (:text elem)))


(defn link
  ([attr] {:type :link :attr attr}))

(defmethod render :link [elem] (format "<link %s/>" (render-attr (:attr elem))))


(defn is_p? [mp]
  (= :p (:type mp)))

(defmethod render :p [elem] (format "<p %s>%s</p>" (render-attr (:attr elem)) (:text elem)))


(defn render-page
  [content]
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  (render (html {:lang "se" }
                (head 
                 (link {:rel "stylesheet" :href "index.css"})
                 (title "dopparedan"))
                (body
                 (div {:id "main"} 
                      (h1 "Idag är: ")
                      (p content)
                      (p "Kolla imorgon för uppdateringar!"))))))

(defn dagar_till_dopparedan
  [] (let [now (t/local-date)
           doppare-dagen (t/local-date "yyyy/MM/dd" (str (t/format "yyyy" now) "/12/24"))]
       (+ 1 ( mod (t/time-between now doppare-dagen :days) 365))))

(defn run [opts]
  (let [dagar (dagar_till_dopparedan)
        text (case dagar
               0 "dopparedan! God jul!"
               (generate-text dagar))]
    (-> text
        (render-page)
        (( fn [x] ( str "<!DOCTYPE html>" x)))
        (println))))