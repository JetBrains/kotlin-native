#include <stdio.h>
#include <stdlib.h>

#include "pi.h"

int main(int argc, char *argv[])
{
    printf("3.");
    for (n = 1; n <= 1000; n += 9)
            printf("%09d", pi_nth_digit(n));
    printf("\n");

    return 0;
}
