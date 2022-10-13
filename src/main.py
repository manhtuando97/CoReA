import numpy as np
import os
from corea import CoreA
import sys
import argparse
import networkx as nx
import time

#parent_directory = os.path.abspath(os.path.join(sys.path[0], os.pardir))
parent_directory = os.path.abspath(os.path.join(sys.path[0], os.pardir))

def main(args):

    directory = parent_directory + '/Hyperedges/' + args.dataset + '-hyperedge-list.pkl'
    h_graph = CoreA(args.dataset, directory)
    h_graph.load_hypergraph(directory)
    h_graph.load_size_distribution()

    h_graph.load_core()
    h_graph.load_strength()
    h_graph.load_influence()

    availability, order, index_start_deg = h_graph.anchor_availability(scheme = args.tie)
    candidates = h_graph.fill_candidate(availability, order, index_start_deg, degeneracy_scheme = args.degeneracy, scheme = args.scheme)
    augmented = h_graph.rank_select(candidates, scheme = args.rank, budget = args.budget)

    f = open('{}/Hypergraph/Augmentation/{}-{}-{}-{}-{}-{}.txt'.format(parent_directory, args.dataset, args.tie, str(args.degeneracy), args.scheme, args.rank, str(args.budget)), 'w')
    for hyperedge in augmented:
        string = ' '.join([str(x) for x in hyperedge])
        f.write(string +'\n')
    f.close()


    return 0

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Core Analysis of Hypergraphs")
    parser.add_argument('dataset', type=str, help='Select dataset for core analysis')
    parser.add_argument('tie', type=str, help='Select the tie-breaking in Step 1-1: random/1I/SI')
    parser.add_argument('degeneracy', type=int, help='Select the degeneracy enforcementin Step 1-2: 0/1')
    parser.add_argument('scheme', type=str, help='Select the selection scheme in Step 1-2: random/IS')
    parser.add_argument('rank', type=str, help='Select the ranking scheme in Step 2: random/mrkc/main')
    parser.add_argument('budget', type=int, help='Budget: number of hyperedges to augment')

    args = parser.parse_args()
    datasets_infor = {'coauth-MAG-Geology': "coauth-MAG-Geology", 'coauth-MAG-History': "coauth-MAG-History",
                      'contact-high-school': "contact-high-school", 'contact-primary-school': "contact-primary-school",
                      'email-Enron': "email-Enron", 'email-Eu': "email-Eu", 'NDC-classes': "NDC-classes",
                      'NDC-substances': "NDC-substances", 
                      'threads-ask-ubuntu': "threads-ask-ubuntu", 'threads-math': "threads-math"}


    if args.dataset in datasets_infor:

        # Directory containing the hypergraph
        #directory =  parent_directory + "/Data/{}/Unique simplices/{}-unique-simplices.txt".format(args.dataset, args.dataset)

        main(args)

    else:
        print('Dataset not found!')
