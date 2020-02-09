# A Graphical Notation for Designing of Ergo Contracts

### Why does it matter? 

[ErgoScript](https://ergoplatform.org/docs/ErgoScript.pdf) is the contracts language on
the Ergo blockchain. Even though it has concise syntax adopted from Scala, it still may
seem confusing at first, because conceptually ErgoScript is quite different from the
conventional languages which we all know and love.

It is because Ergo's programming model is declarative whereas conventional programming is
imperative. In the declarative model of Ergo you need to tell the system what it should do
for you leaving it the freedom of choosing how it will be achieved. The typical question
"How I can send ERGs to Alice in ErgoScript?" should be rephrased as "What I should tell
the system so that Alice gets the coins?". From this perspective ErgoScript is one piece
of the puzzle, and the transactions API of
[Appkit](https://github.com/aslesarenko/ergo-appkit) is the other (we will talk about this
later in this post).

Declarative programming models has already won the battle against imperative programming
in many application domains like Big Data, Stream Processing, Deep Learning, Databases,
etc. In a long term declarative model is also going to win the hearts of dApp developers.

In this post I want to introduce a graphical notation which can help in designing of complex
Ergo contracts in a declarative way.

### From Imperative to Declarative

In the imperative programming model of Ethereum a transaction is a sequence of operations
executed by Ethereum VM. The following [Solidity
function](https://solidity.readthedocs.io/en/develop/introduction-to-smart-contracts.html#subcurrency-example)
implements a transfer of tokens from `sender` to `receiver`. The transaction starts when
`sender` calls this function on an instance of the contract and ends when the function
returns.

```
// Sends an amount of existing coins from any caller to an address
function send(address receiver, uint amount) public {
    require(amount <= balances[msg.sender], "Insufficient balance.");
    balances[msg.sender] -= amount;
    balances[receiver] += amount;
    emit Sent(msg.sender, receiver, amount);
}
```

The function first checks the pre-conditions, then updates the storage (i.e. balances) and
then publish the post-condition as Sent event. The gas consumed by transaction is sent
to the miner as a reward for executing this transaction.

Unlike Ethereum, in Ergo, a transaction is a mapping between input coins which it spends
and output coins which it creates preserving total balances of ERGs and tokens (in which
Ergo is similar to Bitcoin). 

Since Ergo natively support tokens for this specific example of sending tokens we don't
need to write any code in ErgoScript.
What we need instead is to create the 'send' transaction shown in the following figure,
which describe the same token transfer but declaratively.

![Send](send-tx.png)

We need in particular:
1) select unspent sender's boxes, containing in total `tB >= amount` of tokens and `B >=
txFee + minErg` ERGs
2) create one output (box) which is protected by `recipient` public key with `minErg` ERGs and
`amount` of tokens
3) create one _fee_ output protected by the minerFee contract with `txFee` ERGs 
4) create one _change_ output protected by `sender` public key, containing
`B - minErg - txFee` ERGs and `tB - amount` tokens.
5) create a new transaction, sign it using the sender's secret key and send to the Ergo
network.

Note, that all the transaction creation is done off-chain using Appkit Transaction API, thus it
is free of gas. To make it simple Appkit implements a library so
that the basic transactions like this can be created using a single method call.

Thus, when in the Ethereum contract "We send amount from sender to recipient" we literally
changing balances and update storage with the concrete set of commands, and this happens
on-chain. 

In Ergo (as in Bitcoin) transactions are created off-chain and the effects of the
transaction on the blockchain state is that input coins (or Boxes in Ergo's parlance) are
removed and output boxes are added to the
[UTXO](https://en.wikipedia.org/wiki/Unspent_transaction_output) set, this happens
atomically (all or nothing), and no contract code is necessary in this simple example.

However in more complex application scenarios we do need to use ErgoScript code and this
is what we are going to discuss next.

### From Changing State to Checking Context 

In the `send` function example we first check the pre-condition (`require(amount <=
balances[msg.sender],...)`) and then change the state (i.e. update balances
`balances[msg.sender] -= amount`). This is typical in Ethereum transactions, before
we change anything we need to check if it is valid to do at all.

In Ergo, as we discussed, the state (i.e. UTXO set of boxes) is changed implicitly when a
transaction is included in a block. Thus we only need to check the pre-conditions before
the transaction can be added to the block. This is where ErgoScript comes into a play.

It is not possible to "change the state" in ErgoScript because it is a language to check
pre-conditions for spending coins. ErgoScript is purely functional language, without side effects
operating with immutable data values. This means all the inputs, outputs and other
transaction parameters available in a script are immutable. This, among other things,
makes ErgoScript a very simple language, easy to learn and safe to use. Similar to
Bitcoin, each input box contains a script, which should be executed to the `true` value in
order to 1) allow spending of the box (i.e. removing from the UTXO set) and 2) add
the transaction to the block.

It is therefore inaccurate to say that ErgoScript is the language of Ergo contracts,
because it is the language of propositions (of logical predicates, formulas, etc.)
protecting boxes from "illegal" spending. Unlike Bitcoin, in Ergo the whole
transaction content as well as the current blockchain context is available in every
input's script. So each input script may check which outputs are created by the transaction,
their ERG and token amounts. 

While the Ergo's transaction model unlocks the whole range of applications like (DEX, DeFi
Apps, LETS, etc), designing contracts as pre-conditions (or guarding scripts) directly is
not intuitive. In the next section I will introduce useful graphical notation to design
contracts declaratively as diagrams.

### Graphical Notation

The idea behind diagrams is based on the following observations. Ergo boxes are immutable
and cannot be changed. The only thing that can happen with a box is that it can be spent
in a transaction (which in this case should take it as an input). We therefor can draw a
flow of boxes through transactions, so that boxes _flowing in_ to the transaction are
spent and those _flowing out_ are created and added to UTXO. A transaction from this
perspective is a transformer of old boxes to new ones preserving the balances of ERGs and
tokens involved.

The following figure show the main elements of the transaction we already saw previously. 

![Anatomy](tx-anatomy.png)

There is a strictly defined meaning (aka semantics) behind every element of _the diagram_,
so that the diagram is in fact a _formalized specification_, which can be used to
mechanically create and send the corresponding transaction to Ergo blockchain, we will see
this in the next section.

Another way to look at the diagram as an executable spread-sheet-like flow of ERGs and
tokens between boxes, where all transaction balances and conditions are validated.

Now let's look at the pieces of the diagram one by one.

##### 1. Contract Wallet 

This is a key element of the diagram. Every box has a guarding script. Most often it is
the script that contains a public key and checks a signature generated using the
corresponding secret key. This script is trivial in ErgoScript and looks like `{ sender
}`, where sender is the named template parameter. We call the corresponding script template
`pk(pubkey)`, which has one parameter.

_Contract Wallet_ is then a set of all UTXO boxes which have a script with a given
template and given parameter. In the figure, the template is `pk` and parameter `pubkey`
is substituted with `sender' (address or public key).
  
##### 2. Contract

Even though a contract is a property of a box, on the diagram we group the boxes by
their contracts. In the example, we have three instantiated contracts pk(sender),
pk(receiver) and minerFee. Note, that `pk(sender)` is the instantiation of the `pk`
template with the concrete parameter `sender`.

##### 3. Box name

In the diagram we can give each box a name. Besides readability of the diagram, the name
can then be used as a synonym of a more comples an indexed access to the box in the
contract. For example, `change` is the name of the box, which can also be access in the
contract as `OUTPUTS(2)` (though it is not used this way in the example). Box names can
also be used to specify conditions that should be checked in its script (more about it
later).

##### 4. Boxes of the wallet

In the diagram, we show boxes (darker rectangles) as belonging to the contract wallets
(lighter rectangles). Each such _box rectangle_ is connected with a grey _transaction
rectangle_ by either <b color="#ED7D31">orange</b> or <b color="#A9D18E">green</b> arrows
or both. An output box (with incoming green arrow) may include many lines of text where each
line specifies a condition which should be checked as part of the transaction. The first
line specifies the condition on the amount of ERG which should be placed in the box. Other
lines may take one of the following form:
1) `amount: TOKEN` - the box should contain the given `amount` of the given `TOKEN`
2) `R == value` - the box should contain the given `value` of the given register `R`
2) `boxName ? condition` - the box named `boxName` should check `condition` in its script.


##### 5. Amount of ERGs in the box

Each box should store some minimum amount of ERGs which is checked when a transaction is
validated. In the diagram the amount of ERGs is _always_ shown as the first line (e.g. `B:
ERG` or `B - minErg - txFee`). The value type specification `B: ERG` is optional and may
be used for readability. When the value is given as formula, then this formulate should be
respected by the transaction which creates the box.

##### 6. Amount of T token 

A box can store values of many tokens. The tokens on the diagram are named and the `value`
may be associated with the token `T` using `value: T` expression. The `value` may be given
by formula. If the formula is prefixed with a box name like `boxName ? formula`, then it
is also should be checked in the guarding script of the `boxName` box. This additional
specification is very convenient because 1) it allows to validate visual design, and 2) in
many cases the conditions specified in the boxes of a diagram are enough to synthesize the
necessary guarding contracts. (more about this
[below](#from-diagrams-to-ergoscript-contracts))

##### 7. Tx Inputs

Inputs are connected to the corresponding transaction by <b color="#ED7D31">orange</b>
arrows. An input arrow may have a label of the following forms:
1) `name@index` - optional name with an index i.e. `fee@0` or `@2`. This is a property of
the target endpoint of the arrow. The name can be used in conditions of the related boxes
and the `index` is the position of the corresponding box in INPUTS collection of the
transaction.
2) `!action` - is the propety of source of the arrow and gives a name for an alternative
spendings of the box (we will see this in DEX example)

Because of alternative spendings, a box may have many outgoing <b
color="#ED7D31">orange</b> arrows, in which case they should be labeled with different
actions.

##### 8. Transaction

A transaction spends input boxes and creates output boxes. The input boxes are given by
the <b color="#ED7D31">orange</b> arrows and the labels are respected to put inputs at the
right indexes in INPUTS collection. The output boxes are given by the <b
color="#A9D18E">green</b> arrows. Each transaction should preserve a strict balance of ERG
values (sum of inputs == sum of outputs) and for each token the sum of inputs >= the sum
of outputs. The design diagram however requires an explicit specification of the ERG and
token values for all the output boxes to avoid implicit errors and have better readability.

##### 9. Tx Outputs
Outputs are connected to the corresponding transaction by <b color="#A9D18E">green</b>
arrows. An output arrow may have a label of the following form`name@index` - optional name
with an index i.e. `fee@0` or `@2`. This is a property of the source endpoint of the
arrow. The name can be used in conditions of the related boxes and the `index` is the
position of the corresponding box in OUTPUTS collection of the transaction.

### More Complex Example: A Decentralized Exchange (DEX)

### From Diagrams To ErgoScript Contracts

### From Diagrams To Appkit Transactions

### Conclusions

### References

