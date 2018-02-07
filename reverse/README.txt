TP 2017/8
*******

Pour le 12 janvier, en fin de journée vous devrez remettre l'archive 
contenant votre rapport des 3 TPs:
		-EZ.exe
		-HACHOIR.exe
		-CIPHERME.exe
		
	   Nom de l'archive : 	TP_NOM_PRENOM.zip
				Contenu : 	
							Rapport  : (Nom du rapport NOM_PRENOM.pdf)
							Dossier  :  src\
											TP.c (ex: EZ.c, HACHOIR.c)

Pour les scripts vous avez le choix entre c et python.
Mais il n'est pas conseillé de tout faire en python

NB: Les TP sont réservés au personnes du master. Veillez ne pas les diffuser.

EZ.exe
======

Aide:
	Adresse 	fonction
	00401000	check1
	004010C0	check2
	00401130	check3
	004011B0	check4
	00401260	check5
	004019E0	check6

HACHOIRE.exe
============

Ce programme vérifie si vous avez le bon mot de passe.
Il y a le HASH du mot de passe à l'adresse: 0x403018

Vous devez inverser la fonction afin de retrouver le mot de passe.

Aide:
Pour inverser une fonction en assembleur, vous devez:
 - inverser l'ordre des instructions
 - inverser l'instruction (ADD/SUB, SHL/SHR ....) 


Exemple:
	SHR EAX, 0x13
	XOR EAX, 0x37
	SHL EAX, 0xD

L'inverse sera:
	SHR EAX, 0xD
	XOR EAX, 0x37
	SHL EAX, 0x13

CIPHERME.exe
============

Vous avez un programme qui permet de chiffrer des fichiers
Votre objectif est de découvrir comment.
     - Dérivation du mot de passe
     - Génération de l'IV
     - Algo de chiffrement
Vous devez écrire un programme en C permettant de déchiffrer le fichier
fourni en exemple.

Indice: Utilisez un plugin IDA pour la crypto

Aide:
	Adresse    fonction
	
	
	0x40121A    main
	0x403500    hash
	0x4013EA	EAX pointe sur le hash
	0x403600    cipher(plain, cipher, XX, HASH, XX, IV)

Question bonus: 
	- Avec quel language de programmation ce programme a-t-il été écrit ?


Barème 
======

-EZ.exe			5 points
-HACHOIR.exe*	5 points
-CIPHER.exe*	10 points + 1 bonus 

*source obligatoire, pour les autres les sources sont conseillées.


Détails Rapport
===============

Il doit contenir votre démarche bien détaillée pour la résolution des TP.

Ne pas mettre de capture d'écran mais les instructions assembleurs dans 
un bloc code

Vous devez être capables de répondre aux questions suivantes:
		
Quels sont les algorithmes implémentés ?
		-----------------------------------------
		| TP 			|   Nombre d'aglo connu |
		----------------------------------------- 
		| EZ.exe 		|			1			|
		| HACHOIR.exe 	|			0 			|
		| CIPHER.exe 	|			2 			|
		----------------------------------------


Bonus
=====

Des points de bonus pourront être ajoutés selon la qualité du rapport, des 
explications et des scripts.


Malus
=====

Attention à bien respecter les consignes sinon quelques malus peuvent 
être appliqués
	
Si vous ajoutez vos fichiers sources veillez à ce qu'ils compilent bien
Tout fichier source qui ne compile pas entraînera zéro au TP.