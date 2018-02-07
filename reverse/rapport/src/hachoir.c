#include <stdio.h>
#include <stdlib.h>

#define N 3

unsigned int _rotl(const unsigned int value, int shift) {
    return (value << shift) | (value >> (32 - shift));
}

unsigned int _rotr(const unsigned int value, int shift) {
    return (value >> shift) | (value << (32 - shift));
}

void sub_401190_reverse(unsigned int *buf){
    buf[0] ^= 0x444E4152;
    buf[1] ^= 0x53204D4F;
    buf[2] ^= 0x21444545;
}

unsigned int sub_401000_reverse(unsigned int input, int iter){
    unsigned int res = input;
    res ^= 0x1337;
    res += 0x453B698E;
    res = _rotl(res, 7);
    res = ~res;
    res -= iter;
    res = _rotr(res, 13);

    return res;
}

void sub_401110_reverse(unsigned int *buf, int iter){
    int i;
    for(i = 0; i < 3; i++)
    {
        buf[i] = sub_401000_reverse(buf[i], iter);
    }
}

int main(int argc, char **argv)
{
    int i = 0;
//    unsigned int hash[N] = {0xE7BBE069,0xA867B02F,0xB7018C0E};
    unsigned int hash[N] = {0xB7018C0E,0xA867B02F,0xE7BBE069};
    //unsigned int hash[N] = {0x69E0BBE7,0x2FB067A8,0x0E8C01B7};
    //unsigned int hash[N] = {0x0E8C01B7,0x2FB067A8,0x69E0BBE7};
    for(i = 0; i < N; i++)
        printf("0x%08x\n", hash[i]);
    
    sub_401190_reverse(hash);
    for(i = N-1; i >= 0; i--)
        sub_401110_reverse(hash, i);


    for(i = 0; i < N; i++)
        printf("0x%08x\n", hash[i]);
    return 0;
}
