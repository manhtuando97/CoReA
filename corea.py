import numpy as np
import random
from itertools import chain
import scipy
import os
import sys
import scipy
import networkx as nx
import pickle

parent_directory = os.path.abspath(os.path.join(sys.path[0], os.pardir))

'''
CoReA: Improving Core Resilience by Augmentation of Hyperedges
'''

class CoreA:
    def __init__(self, name, directory, tie='random', fill='random', degeneracy=0, rank='random'):
        self.name = name
        self.load_hypergraph(directory)

        self.num_nodes = len(self.nodes)
        self.num_hyperedges = len(self.hyperedges)
        self.size_distribution = []


    def load_hypergraph(self, hypergraph_directory):
        # this function loads hyperedges, initializes core, and counts (exactly) degrees, and initialize coreness (0 or 1 only)
        self.hyperedges_all = []    # all hyperedges
        self.hyperedges = []    # only hyperedges of sizes >= 2
        self.nodes = []
        self.incidence = dict()
        self.degrees = dict()
        self.cores = dict()
        self.max_hyperedge_size = 0

        with open(hypergraph_directory, 'rb') as f:
            hyperedges = pickle.load(f)

        for hyperedge in hyperedges:
            for vertex in hyperedge:
                if int(vertex) not in self.nodes:
                    self.nodes.append(int(vertex))

                    if len(hyperedge) == 1:
                        self.cores[int(vertex)] = 0
                        self.degrees[int(vertex)] = 0
                        self.incidence[int(vertex)] = []
                    elif len(hyperedge) > 1:
                        self.cores[int(vertex)] = 1
                        self.degrees[int(vertex)] = 1
                        self.incidence[int(vertex)] = []
                        self.incidence[int(vertex)].append(hyperedge)

                elif len(hyperedge) > 1:
                    self.degrees[int(vertex)] += 1
                    self.cores[int(vertex)] = 1
                    self.incidence[int(vertex)].append(hyperedge)


            self.hyperedges_all.append(hyperedge)
            if len(hyperedge) > 1:
                self.hyperedges.append(hyperedge)
            if len(hyperedge) > self.max_hyperedge_size:
                self.max_hyperedge_size = len(hyperedge)

        # degree: number of non-trivial hyperedges containing that node
        self.max_node_degree = max(self.degrees.values())
        self.min_node_degree = min(self.degrees.values())

    # add new nodes & hyperedges to current hypergraph. coreness not updated. to update coreness: run self.core_decompose()
    def add_sub_hypergraph(self, add_hyperedges):
        ## add any nodes & hyperedges in the hypergraph
        for hyperedge in add_hyperedges:
            if hyperedge not in self.hyperedges:
                self.nodes = list(set(self.nodes) | set(hyperedge))
                self.hyperedges_all.append(hyperedge)
                if len(hyperedge) > 1:
                    self.hyperedges.append(hyperedge)
                    for node in hyperedge:
                        self.degrees[node] += 1
                        if self.cores[node] == 0:
                            self.cores[node] = 1
                    if len(hyperedge) > self.max_hyperedge_size:
                        self.max_hyperedge_size = len(hyperedge)


        self.num_nodes = len(self.nodes)
        self.num_hyperedges = len(self.hyperedges)

    # load core number
    def load_core(self):
        f = open(parent_directory + '/Hypergraph/Coreness/{}-coreness-rank.txt'.format(self.name), 'r')

        for line in f:
            lines = line.split(' ')
            self.cores[int(lines[0])] = int(lines[1])
        f.close()
        self.degeneracy = max(self.cores.values())


    # load core strengths pre-computed, into dictionary: self.strength
    def load_strength(self):
        f = open(parent_directory + '/Hypergraph/Core Strength/{}-core-strength.txt'.format(self.name), 'r')
        self.strength = dict()
        for line in f:
            lines = line.split(' ')
            self.strength[int(lines[0])] = int(lines[1])
        for node in self.nodes:
            if node not in self.strength.keys():
                self.strength[node] = 1
        return None

    # load core influences pre-computed, into dictionary: self.influence
    def load_influence(self):
        self.influence = self.degrees.copy()
        f = open(parent_directory + '/Hypergraph/Core Influence/{}-core-influence.txt'.format(self.name), 'r')
        for line in f:
            lines = line.split(' ')
            self.influence[int(lines[0])] = float(lines[1])
        for node in self.nodes:
            if node not in self.influence.keys():
                self.influence[node] = 1
        return None

    # load hyperedge size distribution pre-computed
    def load_size_distribution(self):
        with open(parent_directory + '/Hypergraph/Size/{}-size-distribution.pkl'.format(self.name), 'rb') as f:
            self.size_distribution = pickle.load(f)

    # update core strengths: parameter is a list of anchors of the newly added hyperedge
    def update_strengths(self, anchors):
        for vertex in anchors:
            self.strength[vertex] += 1

    # update core influences: no parameters because of using the self.hyperedges
    def update_influences(self):
        new_inf = dict()
        core_degree = dict()

        for node in self.influence.keys():
            new_inf[node] = 1
            core_degree[node] = self.cores[node] + self.strength[node] - 1

        # load hyperedges by their core numbers
        hyperedges_by_core = dict()

        for hyperedge in self.hyperedges:
            if len(hyperedge) > 1:
                min_core = self.cores[hyperedge[0]]

                for node in hyperedge:
                    if self.cores[node] < min_core:
                        min_core = self.cores[node]

                if min_core > 0:
                    anchors = []
                    non_anchors = []

                    # find node lowest redundancy
                    for node in hyperedge:
                        if self.cores[node] == min_core:
                            anchors.append(node)
                        else:
                            non_anchors.append(node)

                    if not min_core in hyperedges_by_core.keys():
                        hyperedges_by_core[min_core] = []
                    hyperedges_by_core[min_core].append((hyperedge, anchors, non_anchors))

        hyperedges_by_core = dict(sorted(hyperedges_by_core.items()))

        for core_num in hyperedges_by_core.keys():
            for hyperedge, anchors, non_anchors in hyperedges_by_core[core_num]:
                # find the anchor with the greatest contribution:
                node = anchors[0]
                contrib = (1 - (self.strength[node] - 1) / core_degree[node]) * new_inf[node]

                for vertex in anchors:
                    contrib_ = (1 - (self.strength[vertex] - 1) / core_degree[vertex]) * new_inf[vertex]
                    if contrib_ > contrib:
                        node = vertex
                        contrib = contrib_

                # update core influence of non-anchors:
                for non_anchor in non_anchors:
                    new_inf[non_anchor] += contrib * (1 + (self.cores[non_anchor] - self.cores[node]) / (self.cores[non_anchor] - 1))
        self.influence = new_inf

    # return a dictionary, value of an node: a number alpha so that if the core influence of that node increases by 1, the objective increases by alpha
    def update_values(self):

        values  = dict()
        core_degree = dict()

        for node in self.influence.keys():
            values[node] = self.strength[node]
            core_degree[node] = self.cores[node] + self.strength[node] - 1

        # load hyperedges by their core numbers
        hyperedges_by_core = dict()

        for hyperedge in self.hyperedges:
            if len(hyperedge) > 1:
                min_core = self.cores[hyperedge[0]]

                for node in hyperedge:
                    if self.cores[node] < min_core:
                        min_core = self.cores[node]

                if min_core > 0:
                    anchors = []
                    non_anchors = []

                    # find node lowest redundancy
                    for node in hyperedge:
                        if self.cores[node] == min_core:
                            anchors.append(node)
                        else:
                            non_anchors.append(node)

                    if not min_core in hyperedges_by_core.keys():
                        hyperedges_by_core[min_core] = []
                    hyperedges_by_core[min_core].append((hyperedge, anchors, non_anchors))

        hyperedges_by_core = dict(sorted(hyperedges_by_core.items(), reverse=True))

        for core_num in hyperedges_by_core.keys():
            for hyperedge, anchors, non_anchors in hyperedges_by_core[core_num]:
                # find the anchor with the greatest contribution:
                node = anchors[0]
                contrib = (1 - (self.strength[node] - 1) / core_degree[node]) * self.influence[node]

                for vertex in anchors:
                    contrib_ = (1 - (self.strength[vertex] - 1) / core_degree[vertex]) * self.influence[vertex]
                    if contrib_ > contrib:
                        node = vertex
                        contrib = contrib_

                # update core influence of non-anchors:
                for non_anchor in non_anchors:
                    values[node] += (1 - (self.strength[node] - 1) / core_degree[node]) * (
                                1 + (self.cores[non_anchor] - self.cores[node]) / (self.cores[non_anchor] - 1))
        self.values = values

    # Step 1-1: core deomposition of the hypergraph and achieve core availability
    # input: scheme of tie-breaking (random, CS/CI,....)
    # availability = core number - degree prior to deletion
    # output: anchor availabilities of nodes, order of deletion, start index of node in the degeneracy
    # load core strength & core influence pre-computed
    def anchor_availability(self, scheme = 'random'):
        # ouptut: anchor availabilities of nodes
        availability = dict()

        # should be changed to degeneracy
        max_deg = self.degeneracy
        # the core
        hyperedges = self.hyperedges.copy()
        degrees = self.degrees.copy()
        incidence = self.incidence.copy()

        # Core Strengths & Core Influence
        strength = self.strength.copy()
        influence = self.influence.copy()

        # the availability, output
        availability = {node: 0 for node in self.degrees.keys()}

        # order of deletion, output
        order = []

        # delete qualified nodes randomly (DONE)
        if scheme == 'random':
            for k in range(1, max_deg+1):
                if k == max_deg:
                    index_start_deg = len(order)
                to_del_nodes = [node for node, deg in degrees.items() if (deg <= k and deg > 0)]
                while len(to_del_nodes) > 0:

                    # from the set of qualified nodes, pop a node randomly
                    node_to_del = np.random.choice(to_del_nodes)
                    to_del_nodes.remove(node_to_del)

                    availability[node_to_del] = k - degrees[node_to_del]
                    order.append(node_to_del)
                    del degrees[node_to_del]

                    affected_nodes = set()
                    for hyperedge in incidence[node_to_del]:
                        for vertex in hyperedge:
                            if vertex != node_to_del:
                                incidence[vertex].remove(hyperedge)
                                degrees[vertex] -= 1
                                affected_nodes.add(vertex)
                        hyperedges.remove(hyperedge)

                    del incidence[node_to_del]

                     # update list of nodes whose degrees <= i
                    for v in affected_nodes:
                        if degrees[v] <= k and (v not in to_del_nodes):
                            to_del_nodes.append(v)


        # delete qualified nodes with chance proportional to CS/CI
        elif scheme == 'SI':
            priorities = dict()
            for node in strength.keys():
                priorities[node] = strength[node] / influence[node]
            for k in range(1, max_deg+1):
                if k == max_deg:
                    index_start_deg = len(order)
                to_del_nodes = [node for node, deg in degrees.items() if (deg <= k and deg > 0)]
                chances = [priorities[node] for node in to_del_nodes]

                while len(to_del_nodes) > 0:

                    probabilities = np.array(chances) / np.sum(chances)
                    node_to_del = np.random.choice(to_del_nodes, size = 1, p = probabilities)[0]

                    # compute anchor availability
                    availability[node_to_del] = k - degrees[node_to_del]
                    order.append(node_to_del)
                    # remove the chosen node
                    to_del_nodes.remove(node_to_del)
                    del degrees[node_to_del]
                    chances.remove(priorities[node_to_del])
                    del priorities[node_to_del]

                    affected_nodes = set()
                    for hyperedge in incidence[node_to_del]:
                        for vertex in hyperedge:
                            if vertex != node_to_del:
                                incidence[vertex].remove(hyperedge)
                                degrees[vertex] -= 1
                                affected_nodes.add(vertex)
                        hyperedges.remove(hyperedge)

                    del incidence[node_to_del]

                     # update list of nodes whose degrees <= i
                    for v in affected_nodes:
                        if degrees[v] <= k & (v not in to_del_nodes):
                            to_del_nodes.append(v)
                            chances.append(priorities[v])

        # delete qualified nodes with chance proportional to 1/CI
        elif scheme == '1I':
            priorities = dict()
            for node in influence.keys():
                priorities[node] = 1/influence[node]

            for k in range(1, max_deg+1):
                if k == max_deg:
                    index_start_deg = len(order)
                to_del_nodes = [node for node, deg in degrees.items() if (deg <= k and deg > 0)]
                chances = [priorities[node] for node in to_del_nodes]
                while len(to_del_nodes) > 0:

                    probabilities = np.array(chances) / np.sum(chances)
                    node_to_del = np.random.choice(to_del_nodes, size = 1, p = probabilities)[0]

                    # compute anchor availability
                    availability[node_to_del] = k - degrees[node_to_del]
                    order.append(node_to_del)
                    # remove the chosen node
                    to_del_nodes.remove(node_to_del)
                    del degrees[node_to_del]
                    chances.remove(priorities[node_to_del])
                    del priorities[node_to_del]

                    affected_nodes = set()
                    for hyperedge in incidence[node_to_del]:
                        for vertex in hyperedge:
                            if vertex != node_to_del:
                                incidence[vertex].remove(hyperedge)
                                degrees[vertex] -= 1
                                affected_nodes.add(vertex)
                        hyperedges.remove(hyperedge)

                    del incidence[node_to_del]

                     # update list of nodes whose degrees <= i
                    for v in affected_nodes:
                        if degrees[v] <= k & (v not in to_del_nodes):
                            to_del_nodes.append(v)
                            chances.append(priorities[v])

        # raise not implemented error
        else:
            raise NotImplementedError('Try random/SI/1I')

        #f.close()
        self.availability = availability
        return availability, order, index_start_deg

    # Step 1-2: form pool of candidate hyperedges
    # input: dictionary for anchor availabilities, order: string for deletion of nodesstring for scheme of filling up, hyperedge size distribution
    # output: pool of candidate hyperedges
    def fill_candidate(self, availability, order, index_start_deg, degeneracy_scheme = 0, scheme = 'random'):
        # should be changed to degeneracy

        degeneracy_nodes = [node for node in self.cores if self.cores[node] == self.degeneracy]
        max_size = len(self.size_distribution) + 1

        candidates = []
        if scheme == 'random':
            for i, node in enumerate(order[:-1]):
                avail = availability[node]
                for i in range(avail):
                    candidate = []
                    candidate.append(node)
                    # draw a size
                    s = np.random.choice(range(2, max_size + 1), size = 1, p = self.size_distribution)[0]
                    alternative_number = min([len(order[i+1:]), s-1])
                    others = np.random.choice(order[i+1:], size=alternative_number, replace = False)
                    for v in others:
                        candidate.append(v)
                    if len(candidate) > 1 and (candidate not in candidates):
                        candidates.append(candidate)

        # sample non-anchors with chance proportional to CI/CS
        elif scheme == 'IS':
            priorities = dict()
            for node in self.strength.keys():
                priorities[node] = self.strength[node] / self.influence[node]

            for i, node in enumerate(order[:-1]):
                chances = []
                for other_node in order[i+1:]:
                    chances.append(priorities[other_node])
                chances = np.array(chances)/np.sum(chances)
                avail = availability[node]
                for j in range(avail):
                    candidate = []
                    candidate.append(node)

                    # draw a size
                    s = np.random.choice(range(2, max_size + 1), size=1, p=self.size_distribution)[0]

                    # fill up with a node in the degeneracy:
                    if degeneracy_scheme == 0:
                        alternative_number = min([len(order[i + 1:]), s-1])
                        others = np.random.choice(order[i + 1:], size=alternative_number, p=chances, replace = False)
                        for v in others:
                            candidate.append(v)
                    else:
                        if i < index_start_deg - 1:
                            degeneracy_node = np.random.choice(degeneracy_nodes, size=1)[0]
                            candidate.append(degeneracy_node)
                            if s > 2:

                                opportunities = chances[:index_start_deg - i-1]
                                opportunities = opportunities / np.sum(opportunities)
                                alternative_number = min([len(order[i + 1:index_start_deg]), s-2])
                                others = np.random.choice(order[i + 1:index_start_deg], size=alternative_number, p=opportunities, replace = False)
                                for v in others:
                                    candidate.append(v)
                        else:
                            alternative_number = min([len(order[i + 1:index_start_deg]), s-2])
                            others = np.random.choice(order[i + 1:], size=alternative_number, p=chances, replace = False)
                            for v in others:
                                candidate.append(v)
                    if len(candidate) > 1 and (candidate not in candidates):
                        candidates.append(candidate)
        else:
            raise NotImplementedError('Try random/IS')
        return candidates

    # Step 2: rank the candidate hyperedges and select the best based on the budget to add
    # input: candidate hyperedges, string for scheme of ranking & selection, budget: #hyperedges to augment
    # output: list of hyperedges to augment to the hypergraph
    def rank_select(self, candidates, scheme='random', budget=10):

        if budget >= len(candidates):
            add_hyperedges = candidates

        else:
            add_hyperedges = []
            if scheme == 'random':
                sel_hyperedges = np.random.choice(candidates, budget)
                for hyperedge in sel_hyperedges:
                    add_hyperedges.append(hyperedge)

            elif scheme == 'mrkc':
                for i in range(budget):
                    scores = []
                    for hyperedge in candidates:
                        min_core = self.cores[hyperedge[0]]
                        for node in hyperedge:
                            if self.cores[node] < min_core:
                                min_core = self.cores[node]
                        anchors = []
                        for node in hyperedge:
                            if self.cores[node] == min_core:
                                anchors.append(node)
                        score = 0
                        for node in anchors:
                            score += self.influence[node] / self.strength[node]
                        scores.append(score)
                    chosen_index = scores.index(max(scores))
                    chosen_hyperedge = candidates[chosen_index]
                    add_hyperedges.append(chosen_hyperedge)
                    candidates.remove(chosen_hyperedge)

                    # append the new hyperedge into the list of hyperedges, update core strengths, update core influences
                    self.hyperedges.append(chosen_hyperedge)
                    self.update_strengths(anchors)
                    self.update_influences()

            elif scheme == 'main':
                self.update_values()
                for i in range(budget):
                    core_degree = dict()

                    for node in self.influence.keys():
                        core_degree[node] = self.cores[node] + self.strength[node] - 1

                    scores = []
                    for hyperedge in candidates:
                        # find the anchors and non-anchors
                        min_core = self.cores[hyperedge[0]]

                        for node in hyperedge:
                            if self.cores[node] < min_core:
                                min_core = self.cores[node]

                        if min_core > 0:
                            anchors = []
                            non_anchors = []

                            # find node lowest redundancy
                            for node in hyperedge:
                                if self.cores[node] == min_core:
                                    anchors.append(node)
                                else:
                                    non_anchors.append(node)

                        # find the contributing anchor
                        node = anchors[0]
                        contrib = (1 - (self.strength[node] - 1) / core_degree[node]) * self.influence[node]

                        for vertex in anchors:
                            contrib_ = (1 - (self.strength[vertex] - 1) / core_degree[vertex]) * self.influence[vertex]
                            if contrib_ > contrib:
                                node = vertex
                                contrib = contrib_

                        score = 0
                        for vertex in anchors:
                            score += self.influence[vertex]
                        for vertex in non_anchors:
                            score += contrib * (1 + (self.cores[vertex] - self.cores[node])/(self.cores[vertex] - 1)) * self.values[vertex]
                        scores.append(score)

                    chosen_index = scores.index(max(scores))
                    chosen_hyperedge = candidates[chosen_index]
                    add_hyperedges.append(chosen_hyperedge)
                    candidates.remove(chosen_hyperedge)

                    # append the new hyperedge into the list of hyperedges, update core strengths, update core influences
                    self.hyperedges.append(chosen_hyperedge)
                    self.update_strengths(anchors)
                    self.update_influences()
                    self.update_values()

            else:
                raise NotImplementedError('Try random/mrkc/main')

        return add_hyperedges


    # write degree-core to a file for further analysis
    def write_degree_core(self, directory, updated_core = None):
        deg_core = nx.DiGraph()
        if updated_core == None:
            cores = self.cores.copy()
        else:
            cores = updated_core
        for node in self.degrees.keys():
            start = self.degrees[node]
            end = cores[node]
            if deg_core.has_edge(start, end):
                deg_core[start][end]['weight'] += 1
            else:
                deg_core.add_edge(start, end, weight=1)

        nx.write_weighted_edgelist(deg_core, directory)
        print("done writing degree-hypercore of {}".format(self.name))

    # compute degeneracy
    def degeneracy(self):
        print('Degeneracy of {} is: {}'.format(self.name, str(max(self.cores.values()))))
        return max(self.cores.values())

    # write into a file the coreness of each node and another file nodes in the degeneracy core
    def write_coreness(self, directory1=None, directory2=None):
        # default: if directory not specified, write to the Results folder
        if directory1 == None:
            directory1 = parent_directory + '/Results/Degree-Hypercore/{}-coreness.txt'.format(self.name, self.name)
        if directory2 == None:
            directory2 = parent_directory + '/Results/Degree-Hypercore/{}-degeneracy-core-nodes.txt'.format(self.name, self.name)
        f = open(directory1, 'w')
        g = open(directory2, 'w')
        max_coreness = self.degeneracy()
        for node in self.cores.keys():
            f.write('{} {}\n'.format(node, self.cores[node]))
            if self.cores[node] == max_coreness:
                g.write(str(node) + '\n')
        f.close()
        g.close()

        return 0


    # write into a file the degeneracy core
    def degeneracy_core(self, directory=None):
        if directory == None:
            directory = parent_directory + '/Results/Degree-Hypercore/{}-degeneracy-core.txt'.format(self.name, self.name)
        f = open(directory, 'w')
        for hyperedge in self.degeneracyCore:
            for i in range(len(hyperedge) - 1):
                f.write(str(hyperedge[i]) + ' ')
            f.write(str(hyperedge[-1]) + '\n')
        print('done writing the degeneracy core of {}'.format(self.name))

