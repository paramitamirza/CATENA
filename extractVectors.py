#!/usr/bin/python

import sys
from decimal import Decimal
import numpy as np
import gensim

token_file = sys.argv[1]

out_file = token_file.replace("tokens", "embedding-word2vec-300")

token_fin = open(token_file, "r")
embed_fout = open(out_file, "w")

model = gensim.models.Word2Vec.load_word2vec_format('GoogleNews-vectors-negative300.bin', binary=True)

for pair in token_fin.readlines():
    cols = pair.strip().split(",")
    if cols[0].lower() in model:
        vec1 = ','.join(['%.6f' % num for num in model[cols[0].lower()]])
    else:
        vec1 = ','.join(['%.6f' % num for num in np.zeros(300)])
    if cols[1] != "O":
        if cols[1].lower() in model:
            vec2 = ','.join(['%.6f' % num for num in model[cols[1].lower()]])
        else:
            vec2 = ','.join(['%.6f' % num for num in np.zeros(300)])
    embed_fout.write(vec1 + ',' + vec2 + ',' + label + '\n')
    
token_fin.close()
embed_fout.close()
    
    
    
