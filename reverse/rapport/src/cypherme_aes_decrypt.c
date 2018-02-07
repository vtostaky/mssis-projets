#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <openssl/evp.h>
#include <openssl/aes.h>

#ifndef TRUE
#define TRUE 1
#endif

#ifndef FALSE
#define FALSE 0
#endif


/**
 *  * Encrypt or decrypt, depending on flag 'should_encrypt'
 *   */
void en_de_crypt(int should_encrypt, FILE *ifp, FILE *ofp, unsigned char *ckey, unsigned char *ivec) {

    const unsigned BUFSIZE=4096;
    unsigned char *read_buf = malloc(BUFSIZE);
    unsigned char *cipher_buf;
    unsigned blocksize;
    int out_len;
    EVP_CIPHER_CTX ctx;

    EVP_CipherInit(&ctx, EVP_aes_128_cbc(), ckey, ivec, should_encrypt);
    blocksize = EVP_CIPHER_CTX_block_size(&ctx);
    cipher_buf = malloc(BUFSIZE + blocksize);

    fseek(ifp,32,SEEK_SET);
    while (1) {
        // Read in data in blocks until EOF. Update the ciphering with each read.
        int numRead = fread(read_buf, sizeof(unsigned char), BUFSIZE, ifp);
        EVP_CipherUpdate(&ctx, cipher_buf, &out_len, read_buf, numRead);
        fwrite(cipher_buf, sizeof(unsigned char), out_len, ofp);
        if (numRead < BUFSIZE) { // EOF
            break;
        }
    }

    // Now cipher the final block and write it out.

    EVP_CipherFinal(&ctx, cipher_buf, &out_len);
    fwrite(cipher_buf, sizeof(unsigned char), out_len, ofp);
    free(cipher_buf);
    free(read_buf);
}

int main(int argc, char *argv[]) {

    unsigned char ckey[16];
    unsigned char ivec[16];
    FILE *fIN, *fOUT;
    int i;

    if (argc != 2) {
        printf("Usage: <executable> <filename>");
        return -1;
    }

    //Decrypt file now

    fIN = fopen(argv[1], "rb"); //File to be read; cipher text
    for(i = 0; i < 16; i++)
    {
        fread(ckey+i, sizeof(char), 1, fIN);
    }
    
    fseek(fIN,16,SEEK_SET);
    for(i = 0; i < 16; i++)
    {
        fread(ivec+i, sizeof(char), 1, fIN);
    }
    fOUT = fopen("decrypted", "wb"); //File to be written; cipher text

    en_de_crypt(FALSE, fIN, fOUT, ckey, ivec);

    fclose(fIN);
    fclose(fOUT);

    return 0;
} 
