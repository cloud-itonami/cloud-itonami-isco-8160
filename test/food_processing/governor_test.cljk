(ns food-processing.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [food-processing.store :as store]
            [food-processing.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-batch! st {:batch-id "batch-1" :product-category :bakery
                                 :allergen-cross-contact-risk? false})
    (store/register-batch! st {:batch-id "batch-2" :product-category :snack
                                 :allergen-cross-contact-risk? true})
    st))

(deftest ok-on-clean-process
  (let [st (fresh-store)
        proposal {:op :process :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-batch
  (let [st (fresh-store)
        proposal {:op :process :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "no-such-batch"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-batch (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :process :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-allergen-cross-contact-release
  (let [st (fresh-store)
        proposal {:op :release :effect :propose :confidence 0.9 :stake :medium}
        v (governor/check {:batch-id "batch-2"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest proceeds-on-release-without-allergen-risk
  (let [st (fresh-store)
        proposal {:op :release :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:ok? v))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :process :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:batch-id "batch-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:batch-id "batch-1" :op :process})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "batch-1"))))
    (is (= 1 (count (store/ledger st))))))
