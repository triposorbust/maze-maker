(ns maze-maker.core
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

;;
;; Geometry Utilities
;;

(defn abs [x] (if (< x 0) (* -1 x) x))

(defn mdist [[x1 y1] [x2 y2]]
  (let [dx (- x1 x2)
        dy (- y1 y2)]
    (+ (abs dx) (abs dy))))

(defn gen-neighbors
  ([p] (gen-neighbors p 1))
  ([[x y] d] (set (for [dx [(- d) 0 d] dy [(- d) 0 d]
                      :when (= d (+ (abs dx) (abs dy)))]
                  [(+ x dx) (+ y dy)]))))

(defn- gen-surrounds [[x y]]
  (for [dx [-1 0 1] dy [-1 0 1]
        :when (not= [0 0] [dx dy])]
    [(+ x dx) (+ y dy)]))

(defn gen-betweens
  [[x1 y1] [x2 y2]]
  (let [ymn (min y1 y2) ymx (max y1 y2)
        xmn (min x1 x2) xmx (max x1 x2)]
    (cond
     (= x1 x2) (for [y (range (inc ymn) ymx)] [x1 y])
     (= y1 y2) (for [x (range (inc xmn) xmx)] [x y1])
     :else nil)))

(defn box-contains? [b [x y]]
  (let [xmn 0 xmx (dec (:width  b))
        ymn 0 ymx (dec (:height b))]
    (and (<= xmn x xmx)
         (<= ymn y ymx))))

;;
;; Initiation + Termination Logic
;;

(defn rand-edge [w h]
  (let [exits (concat (for [x [0 (dec w)] y (range h)] [x y])
                      (for [x (range w) y [0 (dec h)]] [x y]))]
    (rand-nth exits)))

(defn rand-init [w h exit open wall]
  (rand-nth (filter #(> (mdist % exit) (/ (+ w h) 2))
                    (seq (set/difference (set open) (set wall))))))

;;
;; Random Maze Generator
;;

(defn build-random-maze
  ([w h] (build-random-maze w h 0.2))
  ([w h p]
     (let [exit (rand-edge w h)
           open (for [x (range w) y (range h) :when (not= exit [x y])] [x y])
           wall (set (filter (fn [_] (< (rand) p)) open))
           init (rand-init w h exit open wall)]
       { :exit exit :wall wall :init init :width w :height h })))

;;
;; Grid Partition + Logic
;;

(defn- _grid-ofilter [d-pred box orig open]
  (let [h (:height box)
        w (:width box)
        [ox oy] orig]
    (set (for [x (range w) y (range h)
               :let [dx (abs (- ox x))
                     dy (abs (- oy y))]
               :when (d-pred [dx dy])]
           [x y]))))

(defn- grid-ocell [box orig open]
  (let [pred (fn [[dx dy]] (and (even? dx) (even? dy)))]
    (_grid-ofilter pred box orig open)))

(defn- grid-owall [box orig open]
  (let [pred (fn [[dx dy]] (or (odd? dx) (odd? dy)))]
    (_grid-ofilter pred box orig open)))

(defn- grid-neighbors [box p]
  (filter #(box-contains? box %) (gen-neighbors p 2)))

;;
;; DFS w/ Backtracking Maze Generator
;;

(defn- dfs-neighbors [box p unseen]
  (filter #(contains? unseen %) (grid-neighbors box p)))

(defn- build-dfs-wall [box curr st wall unseen]
  (if (seq unseen)
    (let [ns (dfs-neighbors box curr unseen)]
      (cond
       (seq ns) (let [nx (rand-nth ns)
                      bt (set (gen-betweens curr nx))]
                  (recur box nx (conj st curr)
                         (set/difference wall bt)
                         (disj unseen nx)))
       (seq st) (recur box (peek st) (pop st) wall unseen)
       :else    (let [nx (rand-nth (seq unseen))]
                  (recur box nx st wall (disj unseen nx)))))
    wall))

(defn build-dfs-maze [w h]
  (let [mbox {:width w :height h}
        exit (rand-edge w h)
        open (for [x (range w) y (range h) :when (not= exit [x y])] [x y])
        cell (grid-ocell mbox exit open)
        wall (build-dfs-wall mbox exit [] (grid-owall mbox exit open) cell)
        init (rand-init w h exit cell wall)]
    { :width w :height h :exit exit :wall wall :init init }))

;;
;; DLA: Diffusion-limited aggregation
;;

(defn- rand-dla-seed [b wall]
  (let [opt (set/difference
             (set (for [x (range (:width  b))
                        y (range (:height b))]
                    [x y]))
             wall)]
    (rand-nth (seq opt))))

(defn- build-dla-wall 
  ([b n]
     (let [init-wall (rand-dla-seed b #{})
           init-cell (rand-dla-seed b #{init-wall})]
       (build-dla-wall b init-cell #{init-wall} n)))
  ([b p wall n]
     (if (< (count wall) n) ;; termination case
       (if-let [ns (seq (filter (partial box-contains? b)
                                (gen-surrounds p)))]
         (let [nx (rand-nth ns)]
           (if (contains? wall nx)
             (recur b (rand-dla-seed b wall) (conj wall p) n)
             (recur b nx wall n)))
         (recur b (rand-dla-seed b wall) wall n))
       wall)))

(defn build-dla-maze
  ([w h] (build-dla-maze w h 0.19))
  ([w h p]
     (let [nmax (* p w h)
           abox { :width w :height h }
           cell (for [x (range w) y (range h)] [x y])
           wall (build-dla-wall abox nmax)
           open (set/difference (set cell) wall)
           exit (rand-nth (seq open))
           init (rand-init w h exit open wall)]
       { :width w :height h :exit exit :wall wall :init init })))

;;
;; Pretty-Printing Functions
;;

(defn- stringify-posn [m p]
  (cond
   (contains? (:wall m) p) \o
   (= (:init m) p) \s
   (= (:exit m) p) \g
   :else \ ))

(defn- stringify-line [m l]
  (map (partial stringify-posn m) l))

(defn stringify-maze [m]
  (let [posn (for [y (range (:height m)) x (range (:width m))] [x y])
        line (partition-by #(nth % 1) posn)
        char (map (partial stringify-line m) line)]
    (string/join \newline (map #(apply str %) char))))

;;
;; Top-level execution logic.
;;

(defn build-maze [type w h]
  (case type
    :dfs  (build-dfs-maze w h)
    :dla  (build-dla-maze w h)
    :rand (build-random-maze w h)))

(def cli-options
  [["-x" "--width N" "Width of arena"
    :default 21
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 5 %) "Must be an integer greater than 5"]]
   ["-y" "--height M" "Height of arena"
    :default 11
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 5 %) "Must be an integer greater than 5"]]
   ["-t" "--type STR" "Generating algorithm (\"dfs\", \"dla\", \"rand\")"
    :default :rand
    :parse-fn #(keyword %)
    :validate [#(or (= % :rand)
                    (= % :dfs)
                    (= % :dla))
               "Use: \"dfs\", \"dla\", or \"rand\""]]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Utility program for generating arena maps."
        ""
        "Usage: maze-maker [options]"
        ""
        "Options:"
        options-summary
        ""
        "Refer to docs for more details."]
       (string/join \newline)))

(defn error-msg [errors]
  (str "Errors parsing command line call:\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [options arguments summary errors]}
        (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      (not= (count arguments) 0) (exit 0 (usage summary))
      errors (exit 1 (error-msg errors)))
    (println (stringify-maze (build-maze (:type options)
                                         (:width options)
                                         (:height options))))))
