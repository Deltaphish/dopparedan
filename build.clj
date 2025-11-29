(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'dfd)
(def version "1.0.0")
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :ns-compile '[dfd.main]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file jar-file
           :basis @basis
           :main 'dfd.main}))