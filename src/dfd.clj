(ns dfd
  (:require [clojure.string :as str]
            [java-time.api :as t]))

;;; HTML GENERATION ;;;

(defn render-attr
  "Renders a keymap to html attributes
   Aka {:a 1 :b 2} => a=\"1\" b=\"2\""
  [attr]
  (if (map? attr)
    (str/join " " (map
                   (fn [key] (str (name key) "=\"" (key attr) "\""))
                   (keys attr)))
    ""))

(defmulti render (fn [elem] (:tagtype elem)))
(defmethod render :default [_elem] "")

(defn tag? [t]
  (:tagtype t))


(defmacro html-tag
  "Given a string 'x' this macro produces the constructor (x args) (x? arg) and
   an implementation of the render multifunction defined above tied to :x.
   The arguments given to (x args) will be stored as
     a) children if they are also html-tags, and will be recursivly rendered
     b) html-tag attributes if they are a map
     c) text if they are a string
   Not the most performant impl but as this is not run per request that should be fine. 
   "
  [tag-name]
  (list (list 'defn (symbol tag-name)
              (list '[& children] {:tagtype (keyword tag-name)
                                   :children '(filter tag? children)
                                   :text '(filter string? children)
                                   :attr '(first (filter (fn [x] (and (map? x) (not (tag? x)))) children))}))
        (list 'defn (symbol (str (name tag-name) "?")) '[elem]
              (list '= (keyword tag-name) '(:tagtype elem)))
        (list 'defmethod 'render (keyword tag-name) '[elem]
              (list 'format
                    (list 'str (format "<%s " (name tag-name))
                          '(render-attr (:attr elem))
                          ">%s"
                          (format "</%s>" (name tag-name)))
                    '(str/join "" (if (empty? (:text elem))
                                    (map render (:children elem))
                                    (:text elem)))))))

; Generate HTML tags
(html-tag "html")
(html-tag "head")
(html-tag "body")
(html-tag "div")
(html-tag "main")
(html-tag "title")
(html-tag "nav")
(html-tag "p")
(html-tag "h1")
(html-tag "h2")
(html-tag "h3")
(html-tag "h4")
(html-tag "h5")

;; Does currently not support self-closing tags, impl these manually
(defn link
  ([attr] {:tagtype :link :attr attr}))
(defmethod render :link [elem] (format "<link %s/>" (render-attr (:attr elem))))


;;; APP ;;;

(defn dagar_till_dopparedan
  [] (let [now (t/local-date)
           doppare-dagen (t/local-date "yyyy/MM/dd" (str (t/format "yyyy" now) "/12/24"))]
       (+ 1 (mod (t/time-between now doppare-dagen :days) 365))))

(defn generate-text [count]
  (case count
        0 "dopparedan! God jul!"
        (str (str/join (repeat count "dan före "))
             "dopparedan!")))


(defn page
  [content]
  #_{:clj-kondo/ignore [:unresolved-symbol]}
  (html {:lang "se"}
                (head
                 (link {:rel "stylesheet" :href "index.css"})
                 (title "dopparedan"))
                (body
                 (div {:id "main"}
                      (h1 "Idag är: ")
                      (p content)
                      (p "Kolla imorgon för uppdateringar!")))))



(defn run [opts]
  (let [dagar (dagar_till_dopparedan)
        text (generate-text dagar)]
    (-> text
        (page)
        (render)
        ((fn [x] (str "<!DOCTYPE html>" x))) ;; Well this could be done better...
        (println))))

