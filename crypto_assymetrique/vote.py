#!/usr/local/bin/python

from Crypto import Random
from Crypto.Random import random
from Crypto.PublicKey import ElGamal
from Crypto.Util.number import GCD
from Crypto.Hash import SHA
import operator

def keygen():
    key = ElGamal.generate(1024, Random.new().read)
    comps_priv = ('p', 'g', 'y', 'x')
    comps_pub = ('p', 'g', 'y')

    out = "\n".join(["{} = {}".format(comp, getattr(key, comp)) for comp in comps_priv])
    with open("key", 'w') as key_file:
        key_file.write(out)
    
    out = "\n".join(["{} = {}".format(comp, getattr(key, comp)) for comp in comps_pub])
    with open("key.pub", 'w') as key_file:
        key_file.write(out)
    return key


def vote(key, m):
    while 1:
        k = random.StrongRandom().randint(1,key.p-2)
        if GCD(k,key.p-1)==1: break
    cryptuple = key.encrypt(key.g**m, k)
    out = "%s\n" % (cryptuple,)
    with open("vote_crypt", 'a') as vote_crypt:
        vote_crypt.write(out)


def decrypt_vote(key):
    with open("vote_crypt", 'r') as vote_crypt:
        for vc in vote_crypt.readlines():
            vc_tuple = tuple(long(i) for i in vc.strip('()\n').split(', '))
            res = key.decrypt(vc_tuple)
            for j in range(2):
                if key.g**j == res:
                    out = "%s\n" % (j,)
                    with open("vote_clear", 'a') as vote_clear:
                        vote_clear.write(out)
                    break


def count_vote(key, total):
    c = (1, 1)
    with open("vote_crypt", 'r') as vote_crypt:
        for vc in vote_crypt.readlines():
            vc_tuple = tuple(long(i) for i in vc.strip('()\n').split(', '))
            c = tuple((item1 * item2) % key.p for item1, item2 in zip(c, vc_tuple))
    print "final tuple %s"%(c,)
    res = key.decrypt(c)
    for n in range(1,total+1):
        if (key.g**n) % key.p == res:
            out = "total %s\n" % (n,)
            with open("vote_clear", 'a') as vote_clear:
                vote_clear.write(out)
            break


def random_vote(key, num):
    for _ in range(num):
        m = random.randint(0, 1)
        print m
        vote(key, m)


if __name__=="__main__":
    key = keygen()
    random_vote(key,20)
    decrypt_vote(key)
    count_vote(key,20)
