#!/usr/local/bin/python

import sys
from Crypto import Random
from Crypto.Random import random
from Crypto.PublicKey import ElGamal
from Crypto.Util.number import GCD
from Crypto.Hash import SHA
import operator


def largerange(stop):
    i = 0
    while i < stop:
        yield i
        i += 1


def keygen(keyfile):
    key = ElGamal.generate(1024, Random.new().read)
    comps_priv = ('p', 'g', 'y', 'x')
    comps_pub = ('p', 'g', 'y')

    out = ",".join(["{}".format(getattr(key, comp)) for comp in comps_priv])
    with open(keyfile, 'w') as key_file:
        key_file.write(out)
    
    out = ",".join(["{}".format(getattr(key, comp)) for comp in comps_pub])
    with open(keyfile+".pub", 'w') as key_file:
        key_file.write(out)


def keyload(keyfile):
    with open(keyfile, 'r') as key_file:
        for vc in key_file.readlines():
            vc_tuple = tuple(long(i) for i in vc.strip('()\n').split(','))
    key = ElGamal.construct(vc_tuple)
    return key
	

def vote(key, m, crypt):
    while 1:
        k = random.StrongRandom().randint(1,key.p-2)
        if GCD(k,key.p-1)==1: break
    cryptuple = key.encrypt(key.g**m, k)
    out = "%s\n" % (cryptuple,)
    with open(crypt, 'a') as vote_crypt:
        vote_crypt.write(out)


def decrypt_vote(key, crypt, clear):
    with open(crypt, 'r') as vote_crypt:
        for vc in vote_crypt.readlines():
            vc_tuple = tuple(long(i) for i in vc.strip('()\n').split(', '))
            res = key.decrypt(vc_tuple)
            for j in largerange(key.p):
                if (key.g**j) % key.p == res:
                    out = "%s\n" % (j,)
                    with open(clear, 'a') as vote_clear:
                        vote_clear.write(out)
                    break


def count_vote(key, crypt, comb):
    c = (1, 1)
    with open(crypt, 'r') as vote_crypt:
        for vc in vote_crypt.readlines():
            vc_tuple = tuple(long(i) for i in vc.strip('()\n').split(', '))
            c = tuple((item1 * item2) % key.p for item1, item2 in zip(c, vc_tuple))
    out = "%s\n" % (c,)
    with open(comb, 'a') as vote_comb:
        vote_comb.write(out)


def random_vote(key, num, crypt):
    for _ in largerange(num):
        m = random.randint(0, 1)
        vote(key, m, crypt)


if __name__=="__main__":
    if len(sys.argv) == 1:
        print 'Usage:'
        print 'vote.py -keygen <key> : to generate a key pair and store the private key to given key file, and the public key to key.pub file'
        print 'vote.py -key <key> -vote <v> <file> : to generate an encrypted vote from value v, and append the result to given file'
        print 'vote.py -key <key> -decrypt <file> <result> : decrypt all encrypted votes and store result into given result file'
        print 'vote.py -key <key> -randvote <n> <file> : generate n random encrypted votes and save the result by appending to given file'
        print 'vote.py -key <key> -combine <file> <result> : reads file containing encrypted votes, compute the addition of all votes, and save it into given result file'
        print 'Default sequence is running'
        
        keygen("key")
        print 'Private and public keys generated'
        privatekey = keyload("key")
        publickey = keyload("key.pub")
        
        random_vote(publickey,100,"vote_crypt")
        print '100 votes generated'
        count_vote(publickey,"vote_crypt","vote_comb")
        print 'Votes combined'
        decrypt_vote(privatekey,"vote_comb","vote_clear")
        print 'Result decrypted : see vote_clear file.'
        exit()

    if sys.argv[1] == "-keygen":
        if len(sys.argv) < 3:
            print 'Usage:'
            print 'vote.py -keygen <key> : to generate a key pair and store the private key to given key file, and the public key to key.pub file'
            exit()
        keygen(sys.argv[2])
    
    if sys.argv[1] == "-key":
        if (len(sys.argv) != 6
                or (sys.argv[3] != "-vote"
                and sys.argv[3] != "-decrypt"
                and sys.argv[3] != "-randvote"
                and sys.argv[3] != "-combine")):
            print 'Usage:'
            print 'vote.py -key <key> -vote <v> <file> : to generate an encrypted vote from value v, and append the result to given file'
            print 'vote.py -key <key> -decrypt <file> <result> : decrypt all encrypted votes and store result into given result file'
            print 'vote.py -key <key> -randvote <n> <file> : generate n random encrypted votes and save the result by appending to given file'
            print 'vote.py -key <key> -combine <file> <result> : reads file containing encrypted votes, compute the addition of all votes, and save it into given result file'
            exit()

        key = keyload(sys.argv[2])

        if sys.argv[3] == "-vote":
            if not sys.argv[4].isdigit() or int(sys.argv[4]) > 1 :
                print 'Usage:'
                print 'vote.py -key <key> -vote <v> <file> : to generate an encrypted vote from value v (0 or 1), and append the result to given file'
                exit()
            vote(key, int(sys.argv[4]), sys.argv[5])
        if sys.argv[3] == "-decrypt":
            decrypt_vote(key, sys.argv[4], sys.argv[5])
        if sys.argv[3] == "-randvote":
            if not sys.argv[4].isdigit():
                print 'Usage:'
                print 'vote.py -key <key> -randvote <n> <file> : generate n random encrypted votes and save the result by appending to given file'
                exit()
            random_vote(key, int(sys.argv[4]), sys.argv[5])

        if sys.argv[3] == "-combine":
            count_vote(key, sys.argv[4], sys.argv[5])
