(ns food-processing.store
  "SSoT for the ISCO-08 8160 independent food-and-related-products
  machine operator actor. Store is a protocol injected into the
  `food-processing.actor` StateGraph — `MemStore` is the default,
  deterministic, zero-dep backend; a Datomic/kotoba-server-backed
  implementation can be swapped in without touching the actor or
  governor (itonami actor pattern, per ADR-2607011000 / CLAUDE.md
  Actors section).

  Domain:

    batch    — a registered production batch (batchId, productCategory,
               allergenCrossContactRisk? boolean)
    record   — a committed operating record under a batch (recordId,
               batchId, op, payload) — written ONLY via commit-record!,
               never mutated in place
    ledger   — an append-only audit trail of every proposal/verdict/
               disposition, regardless of outcome (commit or hold)")

(defprotocol Store
  (batch [s batch-id])
  (records-of [s batch-id])
  (ledger [s])
  (register-batch! [s batch])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (batch [_ batch-id] (get-in @a [:batches batch-id]))
  (records-of [_ batch-id] (filter #(= batch-id (:batch-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-batch! [s batch]
    (swap! a assoc-in [:batches (:batch-id batch)] batch) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:batches {} :records [] :ledger []} seed)))))
