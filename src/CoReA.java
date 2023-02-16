package util;

import java.io.*;
import java.util.*;
import java.lang.*;

public class CoReA{

    private String name;
    private String tie_scheme; // CS/CI, 1/CI, random
    private String selection_scheme; // CI/CS, random
    private int degeneracy_option; // 1,0
    private String ranking_scheme; // CoReA, CI, random
    private int budget;
    private int batch_size;
    private Queue<Integer> deletion_order;
    
    private HashMap<Integer, Set<Integer>> incidence;
    private HashMap<Integer, int[]> hyperedges;
    private int number_hyperedges;
    private HashMap<Integer, Set<Integer>> anchors;
    private HashMap<Integer, Set<Integer>> non_anchors;
    private HashMap<Integer, Set<Integer>> candidate_anchors;
    private HashMap<Integer, Set<Integer>> candidate_non_anchors;
    private HashMap<Integer, Set<Integer>> hyperedge_by_core;
    private HashMap<Integer, Integer> candidate_coreness;
    private float[] size_distribution;
    private Set<Integer> sizes;

    private Set<Integer> nodes;
    private int[] degree;
    private int[] core_number;
    private int degeneracy;
    private Set<Integer> degeneracy_nodes;
    private int[] core_strength;
    private float[] core_influence;
    private float[] values;
    private float[] chance_cs_ci;
    private float[] chance_ci_cs;
    private float[] chance_one_ci;
    private float[] chance_random; // equal probability for all

    private int[] anchor_availability;
    private int max_node_id;

    // HashMap: key/value is the exact node id/hyperedge id
    // Array: array index is node id/hyperedge id - 1


    public CoReA(String name, int max_id, String tie, String scheme, int degeneracy, String rank, int budget, int batch_size) throws IOException
    {
        this.name = name;
        this.max_node_id = max_id;
        this.tie_scheme = tie;
        this.selection_scheme = scheme;
        this.degeneracy_option = degeneracy;
        this.ranking_scheme = rank;
        this.budget = budget;
        this.batch_size = batch_size;
       
        this.core_number = this.load_core_number();
        this.core_strength = this.load_core_strength();
        this.core_influence = this.load_core_influene();

        this.chance_random = new float[max_node_id];
        Arrays.fill(this.chance_random, 1);

        this.size_distribution = this.load_size_distribution(this.name);
        this.incidence = this.load_hypergraph(this.name);
        this.anchor_availability = this.compute_anchor_availability(this.tie_scheme);
        


    }

    // sampling with probability: choose size elements from candidates, probabilities proportional to weights
    public int[] sample(Set<Integer> candidates, float[] weights, int size){
        int[] sampled = new int[size];

        Set<Integer> candidates_ = new HashSet<Integer>(candidates);
        for(int j=0; j<size;j++){
            float total_weight = 0;
            for(int i: candidates_){
                total_weight += weights[i-1];
                }
            double value = Math.random() * total_weight, weight = 0;
            for(int i: candidates_){
                weight += weights[i-1];
                if (value < weight){
                    sampled[j] = i;
                    candidates_.remove(i);
                    break;
                    }
                }
            
        }
        return sampled;
    }

    public void print_incidence(int final_id){
        for(int node: this.incidence.keySet()){
            if (node < final_id){
                for (int hyperedge_index: this.incidence.get(node)){
                System.out.println(hyperedge_index);
                }
            }
        }
    }

    public HashMap<Integer, Set<Integer>>  load_hypergraph(String name) throws IOException
    {
       
        // incidence
        HashMap<Integer, Set<Integer>> incidence = new HashMap<>();
        String directory = String.format("Data/%s-unique-simplices.txt", name);
        File f = new File(directory);
        int hyperedge_index;
        hyperedge_index = 0;
        HashMap<Integer, int[]> hyperedges = new HashMap<>();
        HashMap<Integer, Set<Integer>> anchors = new HashMap<>();
        HashMap<Integer, Set<Integer>> non_anchors = new HashMap<>();
        HashMap<Integer, Set<Integer>> hyperedge_by_core = new HashMap<>();
        int[] degree = new int[this.max_node_id];

        // initialize degree to be zero
        Arrays.fill(degree, 0);
      
        BufferedReader br = new BufferedReader(new FileReader(f));

        // initialize the incidence Array List
        for(int j = 1; j < this.max_node_id + 1; j++){
            Set<Integer> incidence_node = new HashSet<Integer>();
            incidence.put(j, incidence_node);
        }

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] hyperedge = br_line.split(" ");
            
            int hyperedge_size = hyperedge.length;
            
            int[] hyperedge_ = new int[hyperedge_size];
            
            // parse and form hyperedge, compute hyperedge core number
            int hyperedge_core_number = this.core_number[Integer.valueOf(hyperedge[0])-1];
            for(int index=0; index<hyperedge_size; index++){
                hyperedge_[index] = Integer.valueOf(hyperedge[index]);
                if (this.core_number[hyperedge_[index] - 1] < hyperedge_core_number){
                    hyperedge_core_number = this.core_number[hyperedge_[index]-1];
                }
            }


            // store the hyperedge
            hyperedges.put(hyperedge_index, hyperedge_);

            // store incidence & update degree
            Set<Integer> anchor_list = new HashSet<Integer>();
            Set<Integer> non_anchor_list = new HashSet<Integer>();
            for(int index=0; index<hyperedge_size; index++){
                int node = hyperedge_[index];
                
                degree[node-1]++;

                if (this.core_number[node-1] == hyperedge_core_number){
                    anchor_list.add(node);
                }
                else{non_anchor_list.add(node);}

                // update incidence
                if(incidence.containsKey(node)){
                    Set<Integer> incidence_list = incidence.get(node);
                    incidence_list.add(hyperedge_index);
                    incidence.put(node, incidence_list);
                }
                else{
                    Set<Integer> incidence_list = new HashSet<Integer>();
                    incidence_list.add(hyperedge_index);
                    incidence.put(node, incidence_list);
                }
                
            }
            anchors.put(hyperedge_index, anchor_list);
            non_anchors.put(hyperedge_index, non_anchor_list);

            if (hyperedge_by_core.containsKey(hyperedge_core_number)){
                Set<Integer> hyperedge_by_core_set = hyperedge_by_core.get(hyperedge_core_number);
                hyperedge_by_core_set.add(hyperedge_index);
                hyperedge_by_core.put(hyperedge_core_number, hyperedge_by_core_set);

            }
            else{
                Set<Integer> hyperedge_by_core_set = new HashSet<Integer>();
                hyperedge_by_core_set.add(hyperedge_index);
                hyperedge_by_core.put(hyperedge_core_number, hyperedge_by_core_set);
            }
            hyperedge_index++;
        }
        br.close();
        this.number_hyperedges = hyperedge_index;
        String complete_string = String.format("done with incidence, degree, hyperedges, anchors of %s", name);
        
        this.anchors = anchors;
        this.non_anchors = non_anchors;
        this.degree = degree;
        this.hyperedges = hyperedges;
        this.hyperedge_by_core = hyperedge_by_core;
        return incidence;
    }


    public float[] load_size_distribution(String name) throws IOException
    {
        File currentDir = new File(".");
        File parentDir = currentDir.getParentFile();
        String directory = String.format("Hypergraph/Size/%s-size-distribution.txt", name);
        File f = new File(directory);
        Set<Integer> sizes = new HashSet<Integer>();
        

        BufferedReader br = new BufferedReader(new FileReader(f));
        float[] size_distribution = new float[24];

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] size_probability = br_line.split(" ");
            int size = Integer.valueOf(size_probability[0]);
            float probability = Float.valueOf(size_probability[1]);
            size_distribution[size - 2] = probability;
            if (probability != 0){
                sizes.add(size);
            }
        }
        br.close();
        this.sizes = sizes;
        
        String complete_string = String.format("done with size distribution of %s", name);
        
        
        return size_distribution;
        
    }

       public int[] load_core_number() throws IOException
    {
        File currentDir = new File(".");
        File parentDir = currentDir.getParentFile();
        String directory = String.format("Hypergraph/Coreness/%s-coreness-rank.txt", name);
        File f = new File(directory);
        Set<Integer> nodes = new HashSet<Integer>();
        Set<Integer> degeneracy_nodes = new HashSet<Integer>();
        int degeneracy = 0;
        

        BufferedReader br = new BufferedReader(new FileReader(f));
        int[] core_number = new int[this.max_node_id];
        Arrays.fill(core_number, 1);

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] cn = br_line.split(" ");
            int node = Integer.valueOf(cn[0]);
            int coreness = Integer.valueOf(cn[1]);
            core_number[node - 1] = coreness;
            if (coreness > degeneracy){
                degeneracy = coreness;
            }
            nodes.add(node);
        }
        br.close();

        this.nodes = nodes;
        
        // degeneracy and degeneracy-core nodes
        this.degeneracy = degeneracy;

        for(int index = 0; index < this.max_node_id; index++){
            if (core_number[index] == this.degeneracy){
                degeneracy_nodes.add(index+1);
            }
        }

        this.degeneracy_nodes = degeneracy_nodes;
        String complete_string = String.format("done with core numbers %s", name);
        
        return core_number;
    }


    public int[] load_core_strength() throws IOException
    {
        File currentDir = new File(".");
        File parentDir = currentDir.getParentFile();
        String directory = String.format("Hypergraph/Core Strength/%s-core-strength.txt", name);
        File f = new File(directory);
        

        BufferedReader br = new BufferedReader(new FileReader(f));
        int[] core_strength = new int[this.max_node_id];
        Arrays.fill(core_strength, 1);

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] cs = br_line.split(" ");
            int node = Integer.valueOf(cs[0]);
            int strength = Integer.valueOf(cs[1]);
            core_strength[node - 1] = strength;
        }
        br.close();

        String complete_string = String.format("done with core strengths %s", name);
        
        return core_strength;
    }

    public float[] load_core_influene() throws IOException
    {

        File currentDir = new File(".");
        File parentDir = currentDir.getParentFile();
        String directory = String.format("Hypergraph/Core Influence/%s-core-influence.txt", name);
        File f = new File(directory);
        

        BufferedReader br = new BufferedReader(new FileReader(f));
        float[] core_influence = new float[this.max_node_id];
        float[] chance_cs_ci = new float[this.max_node_id];
        float[] chance_ci_cs = new float[this.max_node_id];
        float[] chance_one_ci = new float[this.max_node_id];

        Arrays.fill(core_influence, 1);

        String br_line;
        while ((br_line = br.readLine()) != null){
            String[] ci = br_line.split(" ");
            int node = Integer.valueOf(ci[0]);
            float influence = Float.valueOf(ci[1]);
            core_influence[node - 1] = influence;
            chance_cs_ci[node - 1] = this.core_strength[node-1] / core_influence[node-1];
            chance_ci_cs[node - 1] = 1/chance_cs_ci[node-1];
            chance_one_ci[node - 1] = 1/core_influence[node-1];
        }
        br.close();

        this.chance_cs_ci = chance_cs_ci;
        this.chance_ci_cs = chance_ci_cs;
        this.chance_one_ci = chance_one_ci;
        String complete_string = String.format("done with core influences %s", name);
        
        return core_influence;

    }

    // core decomposition & compute anchor availabilities
    public int[] compute_anchor_availability(String tie_scheme) throws IOException
    {
    
        // anchor availabilities
        int[] anchor_availability = new int[this.max_node_id];

        // deletion queue
        Queue<Integer> deletion_order = new LinkedList<>();

        // copy all containers for easy computation
        HashMap<Integer, int[]> copy_hyperedges = new HashMap<Integer, int[]>(this.hyperedges);
        HashMap<Integer, Set<Integer>> copy_incidence = new HashMap<Integer, Set<Integer>>(this.incidence);
        int[] copy_degree = Arrays.copyOf(this.degree, this.degree.length);
        Set<Integer> copy_node = new HashSet<Integer>();
        copy_node.addAll(this.nodes);

        for(int k=1;;k++) {

            if(copy_node.isEmpty())
                break;
            Set<Integer> SDel = new HashSet<Integer>();
            for(int i : copy_node) {
                if(copy_degree[i-1] < k) {
                   SDel.add(i);
                }
            }

            // delete nodes of core numbers smaller than k
            while(!SDel.isEmpty()) {
                //final Set<Integer> SDelNew = new HashSet<Integer>();

                // tie-breaking scheme
                if (tie_scheme.equals("SI")){ // CS/CI
                    
                    int[] chosen_node = this.sample(SDel, this.chance_cs_ci, 1);
                    int i = chosen_node[0];

                    // change the loop here to hyperedges, then loop through hyperedges
                    Set<Integer> i_incidence = copy_incidence.get(i);
                    for(int incident_hyperedge_index: i_incidence){
                        
                        int[] incident_hyperedge = copy_hyperedges.get(incident_hyperedge_index);
                        for (int node_idx: incident_hyperedge){
                            if (node_idx != i){
                            Set<Integer> node_incidence = copy_incidence.get(node_idx);
                            node_incidence.remove(incident_hyperedge_index);
                            copy_incidence.put(node_idx, node_incidence);
                            copy_degree[node_idx-1]--;

                            if(copy_degree[node_idx-1] == k-1){
                                SDel.add(node_idx);
                                }
                            }
                            
                        }
                        copy_hyperedges.remove(incident_hyperedge_index);
                    }

                    SDel.remove(i);
                    copy_incidence.remove(i);
                    copy_node.remove(i);
                    deletion_order.add(i);
                    anchor_availability[i-1] = k-1 - copy_degree[i-1];
                }

                else if (tie_scheme.equals("1I")){ // 1/CI
                    
                    int[] chosen_node = this.sample(SDel, this.chance_one_ci, 1);
                    int i = chosen_node[0];

                    // change the loop here to hyperedges, then loop through hyperedges
                    Set<Integer> i_incidence = copy_incidence.get(i);
                    for(int incident_hyperedge_index: i_incidence){
                        
                        int[] incident_hyperedge = copy_hyperedges.get(incident_hyperedge_index);
                        for (int node_idx: incident_hyperedge){
                            if (node_idx != i){
                            Set<Integer> node_incidence = copy_incidence.get(node_idx);
                            node_incidence.remove(incident_hyperedge_index);
                            copy_incidence.put(node_idx, node_incidence);
                            copy_degree[node_idx-1]--;

                            if(copy_degree[node_idx-1] == k-1){
                                SDel.add(node_idx);
                                }
                            }
                            
                        }
                        copy_hyperedges.remove(incident_hyperedge_index);
                    }
                    
                    SDel.remove(i);
                    copy_incidence.remove(i);
                    copy_node.remove(i);
                    deletion_order.add(i);
                    anchor_availability[i-1] = k-1 - copy_degree[i-1];
                }

                else{ // random
                    
                    Integer[] array_SDel = SDel.toArray(new Integer[SDel.size()]);
                    Random rndm = new Random();
                    int rndmNumber = rndm.nextInt(SDel.size());
                    int i = array_SDel[rndmNumber];

                    // change the loop here to hyperedges, then loop through hyperedges
                    Set<Integer> i_incidence = copy_incidence.get(i);
                    for(int incident_hyperedge_index: i_incidence){
                        
                        int[] incident_hyperedge = copy_hyperedges.get(incident_hyperedge_index);
                        for (int node_idx: incident_hyperedge){
                            if (node_idx != i){
                            Set<Integer> node_incidence = copy_incidence.get(node_idx);
                            node_incidence.remove(incident_hyperedge_index);
                            copy_incidence.put(node_idx, node_incidence);
                            copy_degree[node_idx-1]--;

                            if(copy_degree[node_idx-1] == k-1){
                                SDel.add(node_idx);
                                }
                            }
                            
                        }
                        copy_hyperedges.remove(incident_hyperedge_index);
                    }
                    SDel.remove(i);
                    copy_incidence.remove(i);
                    copy_node.remove(i);
                    deletion_order.add(i);
                    anchor_availability[i-1] = k-1 - copy_degree[i-1];
                }

            }
        }

        this.deletion_order = deletion_order;
        String complete_string = String.format("done with anchor availabilities of %s", name);
        
        return anchor_availability;
    }

    // TODO: add attribute degeneracy, write Step 1-2 and Step 2.
    // fill up the candidate hyperedges
    public HashMap<Integer, int[]> construct_hyperedge(String selection_scheme, int degeneracy_option){


        Queue<Integer> construct_queue = new LinkedList<>(this.deletion_order);

        Set<int[]> filled_hyperedges = new HashSet<int[]>();
        HashMap<Integer, int[]> candidate_hyperedges = new HashMap<>();
        HashMap<Integer, Set<Integer>> candidate_anchors = new HashMap<>();
        HashMap<Integer, Set<Integer>> candidate_non_anchors = new HashMap<>();
        HashMap<Integer, Integer> candidate_coreness = new HashMap<>();

        int candidate_index = 0;
        while (!construct_queue.isEmpty()){
            int anchor = construct_queue.remove();
            int availability = this.anchor_availability[anchor-1];

            // for the last node, cannot construct any candidate hyperedges
            if (construct_queue.isEmpty()){
                break;
            }

            for(int i =0; i<availability; i++){
                int[] sampled_size_array = this.sample(this.sizes, this.size_distribution, 1);
                int selected_size = sampled_size_array[0];
                int sampled_size = Math.min(1 + construct_queue.size(), selected_size);
                
                int[] candidate_hyperedge = new int[sampled_size];
                
                candidate_hyperedge[0] = anchor;
                if (selection_scheme.equals("IS")){ // chosen based on CI/CS; 2 options regarding degneracy: enforced and waived
                    
                    if (degeneracy_option == 1){ // degeneracy option enforced
                        int [] degeneracy_node_sampled = this.sample(this.degeneracy_nodes, this.chance_ci_cs, 1);
                        candidate_hyperedge[1] = degeneracy_node_sampled[0];
                        if (sampled_size > 2){
                            // choose from other nodes
                            Queue<Integer> other_nodes = new LinkedList<>(construct_queue);
                            other_nodes.remove(degeneracy_node_sampled[0]);
                            Set<Integer> candidate_nodes = new HashSet<Integer>(other_nodes);
                            int[] chosen_nodes = this.sample(candidate_nodes, this.chance_ci_cs, sampled_size-2);
                            for (int index = 0; index < sampled_size-2; index++){
                                candidate_hyperedge[index+2] = chosen_nodes[index];
                            }
                        }
                    }

                    else{ // degeneracy option waived
                        Set<Integer> candidate_nodes = new HashSet<Integer>(construct_queue);
                        int[] chosen_nodes = this.sample(candidate_nodes, this.chance_ci_cs, sampled_size-1);
                        for (int index = 0; index < sampled_size-1; index++){
                        candidate_hyperedge[index+1] = chosen_nodes[index];
                        }
                    }
                    
                    
                }
                else{
                    
                    Set<Integer> candidate_nodes = new HashSet<Integer>(construct_queue);
                    int[] chosen_nodes = this.sample(candidate_nodes, this.chance_random, sampled_size-1);
                    for (int index = 0; index < sampled_size-1; index++){
                        candidate_hyperedge[index+1] = chosen_nodes[index];
                    }
                }
                
                boolean duplicate = false;
                Arrays.sort(candidate_hyperedge);

                for(int[] curr_candidate:filled_hyperedges){
                    duplicate = Arrays.equals(curr_candidate, candidate_hyperedge);
                    if (duplicate){break;}
                }
                
                if (!duplicate){filled_hyperedges.add(candidate_hyperedge);}
                
                
            }
        }

        /* iterate through all filled hyperedges */
        for(int[] considered_hyperedge: filled_hyperedges){
            
            int hyperedge_coreness = this.degeneracy;   
            Set<Integer> this_anchors = new HashSet<Integer>();
            Set<Integer> this_non_anchors = new HashSet<Integer>();

            for (int t = 0; t< considered_hyperedge.length; t++){
                if (this.core_number[considered_hyperedge[t]-1] < hyperedge_coreness){
                    hyperedge_coreness = this.core_number[considered_hyperedge[t]-1];
                }
            }   

            for (int t = 0; t< considered_hyperedge.length; t++){
                if (this.core_number[considered_hyperedge[t]-1] == hyperedge_coreness){
                    this_anchors.add(considered_hyperedge[t]);
                }
                else{this_non_anchors.add(considered_hyperedge[t]);}
            } 
        
            candidate_anchors.put(candidate_index, this_anchors);
            candidate_non_anchors.put(candidate_index, this_non_anchors);
            candidate_hyperedges.put(candidate_index, considered_hyperedge);
            candidate_coreness.put(candidate_index, hyperedge_coreness);
            candidate_index++;
        }
        this.candidate_anchors = candidate_anchors;
        this.candidate_non_anchors = candidate_non_anchors;
        this.candidate_coreness = candidate_coreness;
        return candidate_hyperedges;
    }

    
    public HashMap<Integer, int[]> rank_select(HashMap<Integer, int[]> candidate_hyperedges, String ranking_scheme, int budget, int batch_size){
        if (budget > candidate_hyperedges.size()){
            return candidate_hyperedges;
        }


        int augmented_index = 0;
        HashMap<Integer, int[]> augmented_hyperedges = new HashMap<>();
        HashMap<Integer, int[]> copy_candidates = new HashMap<>();
        copy_candidates.putAll(candidate_hyperedges);
        

        if (ranking_scheme.equals("main")){
            
            this.update_values();

            int curr_budget = budget;
            while(curr_budget>0){
                Set<Integer> candidate_ids = copy_candidates.keySet();
                int curr_batch_size = Math.min(curr_budget, batch_size);

                float[] scores = new float[candidate_hyperedges.size()];
                Arrays.fill(scores, 0);

                int[] core_degree = new int[max_node_id];
                for (int i=0; i<max_node_id; i++){
                    core_degree[i] = this.core_number[i] + this.core_strength[i] - 1;
                }

                // scoring the candidates
                for(int id: candidate_ids){
                    Set<Integer> candidate_hyperedge_anchors = this.candidate_anchors.get(id);
                    Set<Integer> candidate_hyperedge_non_anchors = this.candidate_non_anchors.get(id);

                    float contrib = 0;
                    int vertex =1;
                    
                    for(int anchor_node: candidate_hyperedge_anchors){
                        float this_contrib = (1- (this.core_strength[anchor_node-1]-1)/core_degree[anchor_node-1]) * this.core_influence[anchor_node-1];
                        if (this_contrib > contrib){
                            vertex = anchor_node;
                            contrib = this_contrib;
                        }
                    }
                    int this_score = 0;
                    for(int anchor_node: candidate_hyperedge_anchors){
                        this_score += this.core_influence[anchor_node-1];
                    }

                    for(int non_anchor_node: candidate_hyperedge_non_anchors){
                        this_score += contrib * (1 + (this.core_number[non_anchor_node-1]-this.core_number[vertex-1])/(this.core_number[non_anchor_node-1]-1)) * this.values[non_anchor_node - 1];
                    }
                    scores[id] = this_score;
                }

                // choose the curr_batch_size best candidates with the highest scores
                for (int turn=0; turn<curr_batch_size;turn++){
                    float metric = 0;
                    int chosen_id = 0;
                    
                    for(int id: candidate_ids){
                        if (scores[id] > metric){
                            metric = scores[id];
                            chosen_id = id;
                        }
                    }
                    
                    augmented_hyperedges.put(augmented_index, copy_candidates.get(chosen_id));
                    augmented_index++;
                    
                    // add to hyperedges, update anchors & non-anchors, and update core strengths here
                    this.hyperedges.put(this.hyperedges.size(), copy_candidates.get(chosen_id));
                    this.anchors.put(this.anchors.size(), candidate_anchors.get(chosen_id));
                    this.non_anchors.put(this.non_anchors.size(), candidate_non_anchors.get(chosen_id));
                    this.update_strengths(this.candidate_anchors.get(chosen_id));

                    // set the respective schore of the chosen candidate as 0 to select other candidates and remove the chosen id to avoid duplicates
                    scores[chosen_id] = 0;
                    candidate_ids.remove(chosen_id);
                    copy_candidates.remove(chosen_id);

                    Set<Integer> hyperedge_index_by_core = new HashSet<Integer>();
                    if (this.hyperedge_by_core.containsKey(this.candidate_coreness.get(chosen_id))){
                        hyperedge_index_by_core = this.hyperedge_by_core.get(this.candidate_coreness.get(chosen_id));
                    }
                    
                    hyperedge_index_by_core.add(this.hyperedges.size()-1);
                    this.hyperedge_by_core.put(this.candidate_coreness.get(chosen_id), hyperedge_index_by_core);
                }

                // update all core influences here
                curr_budget = curr_budget - curr_batch_size;
                this.update_influences();
                this.update_values();
            }

        }

        else if (ranking_scheme.equals("CI")){
            
            int curr_budget = budget;
            while(curr_budget >0){
                int curr_batch_size = Math.min(curr_budget, batch_size);

                Set<Integer> candidate_ids = copy_candidates.keySet();

                float[] scores = new float[candidate_hyperedges.size()];
                Arrays.fill(scores, 0);

                // scoring the candidate hyperedges
                for (int id: candidate_ids){
                    Set<Integer> candidate_hyperedge_anchors = this.candidate_anchors.get(id);
                    for (int anchor:candidate_hyperedge_anchors){
                        scores[id] += this.core_influence[anchor-1];
                    }
                }

                // select batch_size candidates with the highest scores
                for (int turn=0; turn<curr_batch_size;turn++){
                    float metric = 0;
                    int chosen_id = 0;
                    for(int id: candidate_ids){
                        if (scores[id] > metric){
                            metric = scores[id];
                            chosen_id = id;
                        }
                    }
                    augmented_hyperedges.put(augmented_index, copy_candidates.get(chosen_id));
                    augmented_index++;
                    
                    // add to hyperedges, update anchors & non-anchors, and update core strengths here
                    this.hyperedges.put(this.hyperedges.size(), copy_candidates.get(chosen_id));
                    this.anchors.put(this.anchors.size(), candidate_anchors.get(chosen_id));
                    this.non_anchors.put(this.non_anchors.size(), candidate_non_anchors.get(chosen_id));
                    this.update_strengths(this.candidate_anchors.get(chosen_id));

                    // set the respective schore of the chosen candidate as 0 to select other candidates
                    scores[chosen_id] = 0;
                    candidate_ids.remove(chosen_id);
                    copy_candidates.remove(chosen_id);

                    Set<Integer> hyperedge_index_by_core = new HashSet<Integer>();
                    if (this.hyperedge_by_core.containsKey(this.candidate_coreness.get(chosen_id))){
                        hyperedge_index_by_core = this.hyperedge_by_core.get(this.candidate_coreness.get(chosen_id));
                    }
                    
                    hyperedge_index_by_core.add(this.hyperedges.size()-1);
                    this.hyperedge_by_core.put(this.candidate_coreness.get(chosen_id), hyperedge_index_by_core);
                }
                // update all core influences here
                curr_budget = curr_budget - curr_batch_size;
                this.update_influences();
            }
            
        }

        else{ // random
            
            int number_candidates = candidate_hyperedges.size();
            Set<Integer> candidates = new HashSet<Integer>();
            for(int i = 0; i< number_candidates; i++){
                candidates.add(i+1);
            }
            float chance[] = new float[number_candidates];
            Arrays.fill(chance, 1);
            int[] chosen_hyperedges = this.sample(candidates, chance, budget);
            for(int i =0; i<budget;i++){
                augmented_hyperedges.put(i, candidate_hyperedges.get(chosen_hyperedges[i]-1));
            }
        }

        return augmented_hyperedges;
    }
    
    public void update_strengths(Set<Integer> anchors){
        for(int node: anchors){
            this.core_strength[node-1] +=1;
        }
    }

    // update core influences of nodes in the hypergraph
    public void update_influences(){
        float[] new_inf = new float[this.max_node_id];
        int[] core_degree = new int[this.max_node_id];

        for (int index=0; index<max_node_id; index++){
            new_inf[index] = 0;
            core_degree[index] = this.core_number[index] + this.core_strength[index] - 1;
        }

        for(int i=1; i<this.degeneracy; i++){
            if(this.hyperedge_by_core.containsKey(i)){
                for(int hyperedge_index: this.hyperedge_by_core.get(i)){
                    Set<Integer> this_anchors = this.anchors.get(hyperedge_index);
                    Set<Integer> this_non_anchors = this.non_anchors.get(hyperedge_index);

                    float contrib = 0;
                    int vertex = 1;

                    for(int anchor_node: this_anchors){
                        float this_contrib = (1 - (this.core_strength[anchor_node - 1])/(core_degree[anchor_node - 1])) * new_inf[anchor_node - 1];
                        if (this_contrib > contrib){
                            contrib = this_contrib;
                            vertex = anchor_node;
                        }
                    }

                    for (int non_anchor_node: this_non_anchors){
                        new_inf[non_anchor_node-1] += contrib *( 1 + (this.core_number[non_anchor_node-1] - this.core_number[vertex-1]) / (this.core_number[non_anchor_node-1] - 1));
                    }
                }
            }
        }
    }

    // the value of each node is alpha so that if the core influence of that node increases by 1, the objective increases by alpha
    public void update_values(){
        float[] value = new float[this.max_node_id];
        int[] core_degree = new int[this.max_node_id];

        for (int i=0; i<this.max_node_id; i++){
            value[i] = this.core_strength[i];
            core_degree[i] = this.core_number[i] + this.core_strength[i] - 1;
        }

        for(int i=1; i<this.degeneracy; i++){
            if(this.hyperedge_by_core.containsKey(i)){
                for(int hyperedge_index: this.hyperedge_by_core.get(i)){
                    Set<Integer> this_anchors = this.anchors.get(hyperedge_index);
                    Set<Integer> this_non_anchors = this.non_anchors.get(hyperedge_index);

                    float contrib = 0;
                    int vertex = 1;

                    for(int anchor_node: this_anchors){
                        float this_contrib = (1 - (this.core_strength[anchor_node - 1])/(core_degree[anchor_node - 1])) * this.core_influence[anchor_node - 1];
                        if (this_contrib > contrib){
                            contrib = this_contrib;
                            vertex = anchor_node;
                        }
                    }

                    for (int non_anchor_node: this_non_anchors){
                        value[non_anchor_node-1] += ( 1 - (this.core_strength[non_anchor_node-1] - 1) /core_degree[non_anchor_node-1]) *
                        (1+(this.core_number[non_anchor_node-1] - this.core_number[vertex-1]) / (this.core_number[non_anchor_node-1] - 1));
                    }
                }
            }
        }

        this.values = value;
    }

    public void write_results(HashMap<Integer, int[]> augmented_hyperedges) throws IOException{
        File results = new File(   "Results/" + this.name + "-" + this.tie_scheme + "-"
        + this.selection_scheme + "-"
        + String.valueOf(this.degeneracy_option) + "-"
        + this.ranking_scheme + "-"
        + String.valueOf(this.budget) + "-"
        + String.valueOf(this.batch_size) + ".txt" );

        BufferedWriter wr = new BufferedWriter(new FileWriter(results));
        for(int i: augmented_hyperedges.keySet()){
            String str = Arrays.stream(augmented_hyperedges.get(i))
                .mapToObj(String::valueOf)
                .reduce((x, y) -> x + " " + y)
                .get();
            wr.write(str + '\n');
        }

        wr.close();
        
    }
}
