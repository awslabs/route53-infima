# Amazon Route 53 Infima

**Amazon Route53 Infima** is a library for managing service-level fault isolation using [Amazon Route 53][route53]. 

Infima provides a **Lattice** container framework that allows you to categorize each endpoint along one or more fault-isolation dimensions such as availability-zone, software implementation, underlying datastore or any other common point of dependency endpoints may share. 

Infima also introduces a new **ShuffleShard** sharding type that can exponentially increase the endpoint-level isolation between customer/object access patterns or any other identifier you choose to shard on.

Both Infima Lattices and ShuffleShards can also be automatically expressed in Route 53 DNS failover configurations using **AnswerSet** and **RubberTree**.  

You can get started in minutes using ***Maven***. **Route53 Infima** is built on top of the low-level Amazon Route53 client in the AWS SDK for Java.  For support in using and installing the AWS SDK for Java, see:

* [API Docs][docs-api]
* [SDK Developer Guide][docs-guide]
* [AWS SDK Forum][sdk-forum]
* [SDK Homepage][sdk-website]
* [Java Development AWS Blog][sdk-blog]

## Infima Features

### Lattice

An Infima Lattice is an n-dimensional container class that allows you to categorize the endpoints in your service by availability-zone, software-version, underlying datastore or any other point of dependency which may cause a fault spanning one or more endpoints. 

Apart from serving as a container for ShuffleShards and RubberTrees, Lattices may also be used to simulate failures directly. The failure of any dimensional unit, e.g. a particular availability zone, can be simulated. This allows you model and measure resiliency even for complex multi-dimensional dependency configurations. For example an advanced two dimensional lattice containing 18 endpoints in 3 different AZs, using 2 different software implementations, may be represented as;

                               us-east-1a     us-east-1b     us-east-1c
                            +--------------+--------------+--------------+
                            |              |              |              |
                     Python |     A B C    |     G H I    |    M N O     |
                            |              |              |              |
                            +--------------+--------------+--------------+
                            |              |              |              |
                      Ruby  |     D E F    |     J K L    |    P Q R     |
                            |              |              |              |
                            +--------------+--------------+--------------+

and then a fault or outage affecting an availability zone may be simulated;

                    simulateFailure("AvailabilityZone", "us-east-1b") =
                               us-east-1a     us-east-1c
                            +--------------+--------------+
                            |              |              |
                     Python |     A B C    |    M N O     |
                            |              |              |
                            +--------------+--------------+
                            |              |              |
                      Ruby  |     D E F    |    P Q R     |
                            |              |              |
                            +--------------+--------------+
    
or a fault in a software version;

                     simulateFailure("SoftwareImplementation", "Python") =
                               us-east-1a     us-east-1b     us-east-1c
                            +--------------+--------------+--------------+
                            |              |              |              |
                      Ruby  |     D E F    |     J K L    |    P Q R     |
                            |              |              |              |
                            +--------------+--------------+--------------+

In addition to the base Lattice class, which is arbitrarily dimensional, Infima also provides three convenience classes for the common cases;

1. **SingleCellLattice** - a class for setups where all endpoints are in a common compartment. Single Cell lattices offer a convenient way to get some of the benefits of ShuffleShards and RubberTrees without modeling the compartments in your service. 
1. **OneDimensionalLattice** - a class for expressing endpoints that have a single dimension of compartmentalization. This is the most common type of configuration used, with Amazon Web Services availability zones being the most common type of compartmentalization dimension.
1. **TwoDimensionalLattice** - a class for advanced ultra-high-availability cases where availability zone compartments are themselves partitioned orthoganally along other dimensions such as software version or underlying datastore.

### ShuffleShard

In traditional sharding, a customer, or object, or other service-level identifier is isolated to a particular endpoint out of many. For example, a user may be sharded to endpoint "C" out of "N" endpoints ...

                            A | B |*C*| D | E | F | G | H | ... | L | M | N

If there is some kind of problem associated with that user's traffic (for example poisonous requests, or an attack), the impact is constrained to the set of users sharing that shard, or 1/N of our total number of endpoints.

If the client has some built in resilience, such as support for multiple endpoints with short timeouts, and/or uses Route 53 DNS failover to handle endpoint failures, then Shuffle Sharding may exponentially increase the degree of isolation. With ShuffleShards, each identifier (user in the above example) is assigned multiple endpoints. For example user "Alice" may be assigned endpoints "C", "F" and "L";

                            A | B |*C*| D | E |*F*| G | H | ... |*L*| M | N

while user "Bob" may be assigned endpoints "B", "F" and "M";

                            A |*B*| C | D | E |*F*| G | H | ... | L |*M*| N

With a ShuffleShard pattern like this, if the traffic or requests associated with user "Bob" causes a problem to the three endpoints that "Bob" is being assigned, this has little if any impact on "Alice" as she shares only one endpoint with "Bob". The overall service-level impact is dramatically reduced to 1/(N choose K) , where K is the number of endpoints we assign each shuffle shard. For some services with a large enough number of endpoints, it is even possible to assign each user their own unique ShuffleShard.

With Infima, ShuffleShards are also Lattice-aware. The shuffle sharders will take care to assign endpoints from each compartment as appropriate. For example when provided with an availability-zone aware Lattice, the Infima Shuffle Sharders will compute ShuffleShards containing endpoints in each availability-zone.  When given a two dimensional, or higher dimensional, Lattice the shuffle sharders will take care to ensure that every dimensional unit is represented at most once and that there is not an excess of endpoints in any particular axis of potential failure. 

Infima provides two ShuffleShard implementations;

1. **SimpleSignatureShuffleSharder** - This ShuffleSharding implementation uses simple probabilistic hashing to assign each identifier, represented by a byte array, a shuffle shard. The complete list of endpoints should be supplied in lattice form, and you may specify how many endpoints should be chosen from each eligible Lattice compartment. 
1. **StatefulSearchingShuffleSharder** - This ShuffleSharding implementation uses a datastore, provided by you, to record every shuffle shard as they are assigned. This implementation can then use this datastore to enforce guarantees about overlap between shuffle shards. For example you may specify that no two assigned shuffle shards may overlap by more than two endpoints. This implementation will perform an exhaustive search to find any eligible pattern of endpoints remaining to assign new shuffle shards.

### AnswerSet

For resilient service endpoint discovery it is often useful to advertise multiple endpoints to clients. However this can be hard to manage in combination with health checks. AnswerSet is a new set type for expressing Route 53 DNS record sets that depend on many health checks. 

Route 53 DNS Failover natively supports ResourceRecordSets which depend on a single Route 53 health check. Answer Sets conveniently generate a chain of ALIASes as neccessary, forming a logical "AND" series that ensure all associated health checks are passing in order to "reach" a particular answer. For example an answer for "www.example.com"  containing 3 IP addresses; 192.0.2.1, 192.0.2.1 and 192.0.2.1 which depends on the 3 health checks abcd-1111, abcd-2222, abcd-3333 may be encoded as;

                                +-------------------------------------+
                                | Name:        www.example.com        |
                                | healthcheck: abcd-1111              |
                                | AliasTarget: ft23wj.www.example.com |
                                +-------------------------------------+
                                                 |
                                                 v
                                +-------------------------------------+
                                | Name:        ft23wj.www.example.com |
                                | healthcheck: abcd-2222              |
                                | AliasTarget: e6yj81.www.example.com |
                                +-------------------------------------+
                                                 |
                                                 v
                                +-------------------------------------+
                                | Name:        e6yj81.www.example.com |
                                | healthcheck: abcd-3333              |
                                | Records:     192.0.2.1              |
                                |              192.0.2.2              |
                                |              192.0.2.2              |
                                +-------------------------------------+

As Route 53 DNS Failover supports full backtracking when answering queries these chains of aliases can be used in place of any regular resource record set.

### RubberTree

RubberTree is ternary tree class designed for managing elastic resources using Route 53 DNS. RubberTree uses AnswerSets to automatically express ShuffleShards and Lattices in Route 53 DNS configurations, with standby and failover configurations present for endpoint-level, availability-zone level faults or any other level you may care to define. 

RubberTree will convert Lattices and ShuffleShards into weighted series of records that depend on the right Route 53 health checks. RubberTree will compute standby records that are prepared for the case of any single endpoint failure, and for faults with any unit in your dimensional Lattices (e.g. a particular availability zone). RubberTree takes care of the complex configuration needed to support endpoint discovery that is always prepared for faults. 

For example, given a One Dimensional Lattice with the following configuration;

                                     us-east-1a  us-east-1b     
                                     +---------+----------+
                                     |  A   B  |  C   D   |     
                                     +---------+----------+

and a maximum number of records per answer of 3, RubberTree will compute a tree of the form;

                              www.example.com
                                     |
         +------------+---------------------------+---------------+-------------------+
         |            |              |            |               |                   |
    weight: 1     weight: 1      weight: 1    weight: 1       weight: 0           weight: 0
    [ A B C ]     [ A B D ]      [ A C D ]    [ B C D ]           |                   |
                                                                  |                   |
                                                      secondary.www.example.com       |
                                                                  |                   |
                                                                  |       secondary.www.example.com
                                                                  |                   |
                                                         us-east-1a.secondary         |
                                                                 |                    |
                                                              [ A B ]        us-east-1b.secondary
                                                                                      |
                                                                                   [ C D ]


As Route 53 follows zero-weighted branches only when there are no other healthy options, this configuration makes every effort to return as many healthy endpoints as possible. The RubberTree class handles all of the complex configuration.

## Getting Started

1. **Sign up for AWS** - Before you begin, you need an AWS account. Please see the [AWS Account and Credentials][docs-signup] section of the developer guide for information about how to create an AWS account and retrieve your AWS credentials.
1. **Minimum requirements** - To run the SDK you will need **Java 1.6+**. For more information about the requirements and optimum settings for the SDK, please see the [Java Development Environment][docs-signup] section of the developer guide.
1. **Install the Amazon Route 53 Infima Library** - Using ***Maven*** is the recommended way to install the Amazon Route 53 Infima Library and its dependencies, including the AWS SDK for Java.  To download the code from GitHub, simply clone the repository by typing: `git clone https://github.com/awslabs/route53-infima`, and run the Maven command described below in "Building From Source".

## Building From Source

Once you check out the code from GitHub, you can build it using Maven.  To disable the GPG-signing in the build, use: `mvn clean install -Dgpg.skip=true`

[sdk-install-jar]: http://sdk-for-java.amazonwebservices.com/latest/aws-java-sdk.zip
[aws]: http://aws.amazon.com/
[route53]: http://aws.amazon.com/route53
[route53-forum]: https://forums.aws.amazon.com/forum.jspa?forumID=87
[sdk-website]: http://aws.amazon.com/sdkforjava
[sdk-forum]: http://developer.amazonwebservices.com/connect/forum.jspa?forumID=70
[sdk-blog]: https://java.awsblog.com/
[sdk-license]: http://aws.amazon.com/asl/
[docs-api]: http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html
[docs-signup]: http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-setup.html
[aws-iam-credentials]: http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/java-dg-roles.html
[docs-guide]: http://docs.aws.amazon.com/AWSSdkDocsJava/latest/DeveloperGuide/welcome.html
