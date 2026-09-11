(ns food-processing.governor
  "ProcessingGovernor — the independent safety/traceability layer for
  the ISCO-08 8160 independent food-processing-machine-operator actor.
  Wired as its own `:govern` node in `food-processing.actor`'s
  StateGraph, downstream of `:advise` — the Advisor has no notion of
  batch provenance or allergen risk, so this MUST be a separate system
  able to reject a proposal (itonami actor pattern, per ADR-2607011000 /
  CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. batch provenance   — the request's batch must be registered.
    2. no-actuation       — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off):
    3. `:release` op on a batch flagged `allergen-cross-contact-risk?`
       true — a batch cannot be released to market without an explicit
       human allergen-labelling review.
    4. low confidence (< `confidence-floor`)."
  (:require [food-processing.store :as store]))

(def confidence-floor 0.6)

(defn- hard-violations [{:keys [request proposal]} batch-record]
  (cond-> []
    (nil? batch-record)
    (conj {:rule :no-batch :detail (str "未登録 batch " (:batch-id request))})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn- allergen-release-risk? [batch-record proposal]
  (and (:allergen-cross-contact-risk? batch-record) (= :release (:op proposal))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `food-processing.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [batch-record (store/batch store (:batch-id request))
        hard (hard-violations {:request request :proposal proposal} batch-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky? (boolean (and batch-record (allergen-release-risk? batch-record proposal)))]
    {:ok? (and (not hard?) (not low?) (not risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky?))}))
