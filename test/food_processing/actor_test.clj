(ns food-processing.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [food-processing.actor :as actor]
            [food-processing.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-batch! st {:batch-id "batch-1" :product-category :bakery
                                 :allergen-cross-contact-risk? false})
    (store/register-batch! st {:batch-id "batch-2" :product-category :snack
                                 :allergen-cross-contact-risk? true})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:batch-id "batch-1" :op :process :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "batch-1"))))))

(deftest holds-on-unregistered-batch-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:batch-id "no-such-batch" :op :process :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-batch")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; :release on the allergen-risk batch-2 always escalates (governor invariant)
        request {:batch-id "batch-2" :op :release :stake :medium}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "batch-2")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "batch-2")))))))
