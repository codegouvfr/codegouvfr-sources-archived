#!/usr/bin/env bb

;; Get all forges by domain names
(def orgs
  (->>
   (yaml/parse-string (slurp "comptes-organismes-publics.yml") :keywords false)
   (group-by #(last (re-find #"https://([^/]+)" (first %))))))

(defn spt! [s] (spit "output.yml" s :append true))

(defn write-props! [forge]
  (doseq [[group properties] (second forge)]
    (when-let [g (last (re-find #"https://[^/]+/([^:]+)" group))]
      (spt! (str "    " g ":\n"))
      (when-let [pso (first (get properties "service_of"))]
        (spt! (str "      pso: " pso "\n")))
      (when-let [pso_id (first (get properties "pso_id"))]
        (spt! (str "      pso_id: " pso_id "\n")))
      (when-let [floss (first (get properties "floss_policy"))]
        (spt! (str "      floss_policy: " (first (get properties "floss_policy")) "\n"))))))

(doseq [forge orgs]
  ;; For each forge, spit its domain name:
  (spt! (str "\n" (first forge) ":\n"))
  ;; For each general purpose forge, list groups as such:
  (if-let [n (re-find #"github.com|gitlab.com|sr.ht|framagit.org|gitlab.adullact.net|gitlab.ow2.org" (first forge))]
    (do (spt! "  general_purpose: true\n")
        (spt! (str "  forge: " (condp = n
                                 "github.com" "github"
                                 "sr.ht"      "sourcehut"
                                 "gitlab") "\n"))
        (spt! "  groups:\n")
        (write-props! forge))
    (do (spt! "  forge: gitlab\n")
        (write-props! forge))))
