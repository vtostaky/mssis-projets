#!/usr/bin/env python

from Crypto.Cipher import AES
import Crypto.Cipher.AES


if __name__ == '__main__':

    crypt_file = open('secret.jpg.bin','rb')
    clear_file = open('secret.jpg','wb')

    key = crypt_file.read(16)
    IV = crypt_file.read(16)
    print key
    print IV
    decipher = AES.new(key, AES.MODE_CBC, IV)
    ciphertext = crypt_file.read(16)
    while ciphertext != "":
        plaintext = decipher.decrypt(ciphertext)
        (plaintext).encode('hex')
        clear_file.write(plaintext)
        ciphertext = crypt_file.read(16)

    crypt_file.close()
    clear_file.close()
