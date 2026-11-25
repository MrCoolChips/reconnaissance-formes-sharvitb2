# Projet Reconnaissance des Formes — SharvitB2 (M1 Informatique, Université Paris Cité)

Java pur (JDK ≥ 17, aucune bibliothèque externe), exécution en ligne de commande.

## Arborescence

```
ProjetRF/
├── run.ps1                      compilation + exécution (Windows PowerShell)
├── src/
│   ├── model/        Methode (ART, ZRK, GFD, YNG, SA), Echantillon, Dataset
│   ├── io/           DatasetLoader (lecture SxxNyyy.MET), CsvWriter, Affichage
│   ├── distance/     Distance, Minkowski (L1, L2, Lp, Linf)
│   ├── classifier/   KPlusProchesVoisins (2.a), NueesDynamiques (2.b), VoteMajoritaire (2.c),
│   │                 Normaliseur, ConfigKnn, Prediction, ...
│   ├── evaluation/   Decoupage (LOOCV, K plis stratifiés), EvaluationEngine, MatriceConfusion,
│   │                 AffectationHongroise, PrecisionRappel, MoteurRecherche, CourbePR
│   └── experiment/   Main + une classe par expérience (Knn, Nuees, Vote, PrecisionRappel, Echelle)
└── results/          fichiers CSV + journal.txt (copie de la sortie console)
```

## Compilation / exécution

```powershell
.\run.ps1
```

ou à la main :

```powershell
javac -encoding UTF-8 --release 17 -d out (Get-ChildItem -Recurse -Filter *.java src).FullName
java -cp out experiment.Main --donnees exemples\=Signatures --sortie results --experience tout
```

Options : `--donnees DIR`, `--sortie DIR`, `--graine N` (défaut 42),
`--experience tout|knn|nuees|vote|pr|echelle`.

## Fichiers produits (`results/`)

| Fichier | Contenu |
|---|---|
| `knn_grille_6classes.csv` | T_reco LOOCV pour k ∈ {1..11}, L1/L2, sans/avec z-score, 5 méthodes |
| `knn_<M>_confusion_6classes.csv`, `knn_<M>_metriques_6classes.csv` | matrice de confusion et P/R/F1 par classe (config retenue) |
| `knn_protocoles_6classes.csv`, `vote_protocoles_6classes.csv` | LOOCV vs K plis stratifiés (K = 12, 6, 4, 3, 2, ×10) |
| `kmeans_resultats.csv`, `kmeans_confusion_*`, `kmeans_metriques_*` | nuées dynamiques : J, itérations, T_reco (hongrois), pureté |
| `vote_*` | vote simple / pondéré, détails des votes par échantillon (6/12/18 classes) |
| `pr_courbes.csv`, `pr_auc.csv` | courbes précision/rappel K = 12 (modes « recherche » et « scores ») et AUC |
| `echelle_6_12_18.csv` | T_reco et temps pour 6, 12, 18 classes (k-PPV par méthode, vote) |

## Remarques

* Le fichier `s05n011.zrkt.txt` de la base comporte une faute de frappe d'extension : le
  chargeur le reconnaît comme ZRK et affiche un avertissement.
* La normalisation (z-score) est toujours estimée sur l'apprentissage du pli courant.
* Les poids du vote pondéré sont les taux leave-one-out individuels calculés sur
  l'apprentissage uniquement (aucune fuite du test).
