#include "transform.h"

__device__ double op(double d1,double *params) {
        return sqrt(d1);
}

extern "C"
__global__ void sqrt_strided_double(int n,int idx,double *dy,int incy,double *params,double *result,int blockSize) {
       transform(n,idx,dy,incy,params,result,blockSize);

 }