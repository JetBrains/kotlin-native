# Torch demo

Executes basic matrix and vector operations on the [Torch](http://torch.ch) backend.
Like other Torch clients, most prominently [PyTorch](http://pytorch.org),
this example is built on top of the 
[Torch C API](https://github.com/torch/torch7/tree/master/lib/TH),
showing how a Torch client for Kotlin/Native could look like.

## Installation

    ./downloadTorch.sh

will install [Torch for C](https://github.com/torch/torch7/tree/master/lib/TH), into
`$HOME/.konan/third-party/torch` (if not yet done). 

To build use `../gradlew build` or `./build.sh`.
    
Then run 

    ../gradlew run
    
Alternatively you can run artifact directly 

    ./build/konan/bin/torch/HelloTorch.kexe