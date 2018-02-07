Le fichier vote.py est un script qui implémente le système de base, sans les améliorations.

Il est écrit en python 2.0 et se base sur le module ElGamal de la librairie
PyCrypto.

Il s'utilise de la manière qui suit :

./vote.py <arguments et options>

Exécuter vote.py sans argument exécute la séquence suivante :
- génération d'une paire de clés
- création d'un fichier key contenant la clé privée
- création d'un fichier key.pub contenant la clé publique
- génération de 100 votes aléatoires chiffrés et aggrégation dans le fichier
  vote_crypt
- combination des votes générés pour calculer l'addition chiffée des votes
  et mettre le résultat dans le fichier vote_comb
- déchiffrement du contenu du fichier vote_comb et écriture du résultat dans
  le fichier vote_clear

Note : pour la fonctionnalité de combination, j'ai choisi d'utiliser la clé
publique, ce qui permet de faire des multiplications modulo p, et ainsi
réduire la complexité et le temps des calculs effectués.

Pour l'utilisation unitaire des fonctionnalités, une aide contextuelle est
fournie par le script.
