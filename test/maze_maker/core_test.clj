(ns maze-maker.core-test
  (:require [clojure.test :refer :all]
            [maze-maker.core :refer :all]))

(deftest test-abs
  (testing "Should absolute value."
    (is (= 1 (abs -1)))
    (is (= 1 (abs  1)))
    (is (= 0 (abs  0)))))

(deftest test-neighbors
  (testing "Should generate lateral neighbors."
    (is (= (count (gen-neighbors [5 12])) 4))
    (is (contains? (gen-neighbors [5 12]) [4 12]))
    (is (contains? (gen-neighbors [5 12]) [6 12]))
    (is (contains? (gen-neighbors [5 12]) [5 11]))
    (is (contains? (gen-neighbors [5 12]) [5 13])))
  (testing "Should enable hops of defined length."
    (is (= (count (gen-neighbors [5 12] 2)) 4))
    (is (contains? (gen-neighbors [5 12] 2) [3 12]))
    (is (contains? (gen-neighbors [5 12] 2) [7 12]))
    (is (contains? (gen-neighbors [5 12] 2) [5 10]))
    (is (contains? (gen-neighbors [5 12] 2) [5 14]))))

(deftest test-betweens
  (testing "Should produce seq of squares between."
    (is (= (count (gen-betweens [5 10] [5 8])) 1))
    (is (contains? (set (gen-betweens [5 10] [5 8])) [5 9]))))

(deftest test-contains
  (testing "Should identify points in- and out-of-bounds."
    (let [box { :width 10 :height 5 }]
      (is (box-contains? box [0 0]))
      (is (box-contains? box [3 2]))
      (is (box-contains? box [9 4]))
      (is (not (box-contains? box [-1 0])))
      (is (not (box-contains? box [0 -1])))
      (is (not (box-contains? box [10 3])))
      (is (not (box-contains? box [0  5]))))))

(deftest test-edge
  (testing "Should return a point on map edge."
    (let [[x y] (rand-edge 10 20)]
      (is (or (= x 0) (= x  9)
              (= y 0) (= y 19))))))

(def test-stringify
  (testing "Should render maze in ASCII."
    (let [maze { :width 4 :height 3
                 :wall #{ [0 0] [1 1] [2 2] [3 1]}
                 :init [2 0]
                 :exit [0 2] }]
      (is (= "o s \n o o\ng o "
             (stringify-maze maze))))))
