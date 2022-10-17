# Improving the Core Resilience of Real-world Hypergraphs: Concepts, Observations, and Algorithms
Source code for the paper **Improving the Core Resilience of Real-world Hypergraphs: Concepts, Observations, and Algorithms**, where we formulate and study the problem of improving the resilience of hypergraphs through the means of augmention hyperedges.
In light of several empirical patterns regarding the core resilience, we propose **CoReA** (<ins><strong>CO</strong></ins>re <ins><strong>Re</strong></ins>silience Improvement by Hyperedge <ins><strong>A</strong></ins>ugmentation).
**CoReA** not only proves theoretically sound but also empirically effective in enhancing the core resilience of real-world hypergraphs.
Extensive experiments on ten real-world datasets demonstrates the consistent superiority of **CoReA** over the baselines.

Note: if a preview of the supplementary materials PDF file does not appear properly, please download the file.

## Datasets
The datasets can be found in the *Hyperedges* folder. 
We remove any duplicate hyperedges and isolated nodes before experimenting with the methods.

Source:
- coauth-MAG-Geology: https://www.cs.cornell.edu/~arb/data/coauth-MAG-Geology/
- coauth-MAG-History: https://www.cs.cornell.edu/~arb/data/coauth-MAG-History/
- contact-high-school: https://www.cs.cornell.edu/~arb/data/contact-high-school/
- contact-primary-school: https://www.cs.cornell.edu/~arb/data/contact-primary-school/
- email-Enron: https://www.cs.cornell.edu/~arb/data/email-Enron/
- email-Eu: https://www.cs.cornell.edu/~arb/data/email-Eu/
- NDC-classes: https://www.cs.cornell.edu/~arb/data/NDC-classes/
- NDC-substances: https://www.cs.cornell.edu/~arb/data/NDC-substances/
- threads-ask-ubuntu: https://www.cs.cornell.edu/~arb/data/threads-ask-ubuntu/
- threads-math: https://www.cs.cornell.edu/~arb/data/threads-math-sx/

The pre-computed statistics of the hypergraphs are in the *Hypergraph* folder:
- Augmentation: where the augmented hyperedges are recorded.
- Core Influence: where the core influences are recorded.
- Core Strength: where the core strengths are recorded.
- Coreness: where the core numbers are recorded.
- Size: where the hyperedge size distributions are recorded.

## Code
The main implementation is in *corea.py*.

## How to run the code:
starting at the *src* folder, run the command:

*python main.py dataset tie degeneracy scheme rank budget*
- dataset: name of the dataset.
- tie: a string represents the tie-breaking scheme in Step 1-1 of *CoReA*: "SI"/"1L"/"random" for CS/CI, 1/CI, and Random, respectively.
- degeneracy: an integer represents whether the degeneracy-node requirement is enforced in Step 1-2 of *CoReA*: 1 for enforced, and 0 for waived.
- scheme: a string represents the selection scheme in Step 1-2 of *CoReA*: "IS"/"random" for CI/CS and Random, respectively.
- rank: a string represents the scoring method in Step 2 of *CoReA*: "main"/"mrkc"/"random" for the Core Influence-Strength (as of our method), MRKC, and Random, respectively.
- budget: an integer represents the budget.

For example: python main.py email-Enron 1I 0 IS random 10.

This code finds 10 augmented hyperedges for the 'email-Enron' dataset: tie-breaking T in Step 1-1 is 1/CI, the degeneracy in Step 1-2 is waived, the selection step in Step 1-2 is CI/CS, the scoring method in Step 2 is Random.

The augmented hyperedges are recorded in 'Hypergraph', with the name of the file as "email-Enron-1I-0-IS-random-10.txt", representing the dataset and the experiment configurations.
