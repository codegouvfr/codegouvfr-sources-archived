#!/usr/bin/env bb

;; Set the spec file name
(def spec-filename "comptes-organismes-publics_new_specs.yml")

;; Remove output file if left over
(fs/delete-if-exists spec-filename)

;; Get all forges by domain names
(def orgs
  (->>
   (yaml/parse-string (slurp "comptes-organismes-publics.yml") :keywords false)
   (group-by #(last (re-find #"https://([^/]+)" (first %))))))

(defn spt! [s] (spit "comptes-organismes-publics_new_specs.yml" s :append true))

(def general-purpose-forges
  '("github.com" "gitlab.com" "sr.ht" "framagit.org" "gitlab.adullact.net" "gitlab.ow2.org"))

(defn write-props! [forge & without-groups?]
  (let [re (if without-groups? #"https://([^/]+)" #"https://[^/]+/([^:]+)")]
    (doseq [[group properties] (second forge)]
      (when-let [g (last (re-find re group))]
        (when-not without-groups? (spt! (str "    " g ":\n")))
        (let [indent (if without-groups? "  " "      ")]
          (when-let [d (get properties "ignored_since")]
            (spt! (str indent "ignored_since: "
                       (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") d) "\n")))
          (when-let [pso (first (get properties "service_of"))]
            (spt! (str indent "pso: " pso "\n")))
          (when-let [pso_id (first (get properties "pso_id"))]
            (spt! (str indent "pso_id: " pso_id "\n")))
          (when-let [floss (first (get properties "floss_policy"))]
            (spt! (str indent "floss_policy: " (first (get properties "floss_policy")) "\n"))))))))

(doseq [forge orgs]
  ;; For each forge, spit its domain name:
  (spt! (str "\n" (first forge) ":\n"))
  ;; For each general purpose forge, list groups as such:
  (if-let [n (re-find (re-pattern (str/join "|" general-purpose-forges)) (first forge))]
    ;; Handle non general purpose forges:
    (do (spt! "  general_purpose: true\n")
        (spt! (str "  forge: " (condp = n
                                 "github.com" "github"
                                 "sr.ht"      "sourcehut"
                                 ;; If forge is set, get it, otherwise assume GitLab
                                 (when-let [[_ properties] (second forge)]
                                   (let [forge (or (get properties "forge") "gitlab")]
                                     forge)))
                   "\n"))
        (spt! "  groups:\n")
        (write-props! forge))
    ;; Assume other non general_purpose forges are GitLab for now
    (do (when-let [[_ properties] (first (second forge))]
          (let [forge (or (get properties "forge") "gitlab")]
            (spt! (str "  forge: " forge "\n"))))
        (write-props! forge true))))
