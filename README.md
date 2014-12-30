# Are We Consistent Yet?

Observed and documented eventual consistency of object stores, e.g., Amazon S3,
OpenStack Swift.

## What is eventual consistency?

Traditional systems provide
[strong consistency](https://en.wikipedia.org/wiki/Strong_consistency), where
clients can immediately view updates.
Some distributed systems relax their consistency model to allow greater
availability or better performance.
[Eventual consistency](https://en.wikipedia.org/wiki/Eventual_consistency)
manifests itself to clients as stale views of data.

## Observed consistency

We ran a test in which we wrote (i.e., create, update, delete) an object and
then attempted to read the object. Across many trials, we count the number of
times the object was not immediately available. When the object is not
immediately found, it's an occurrence of observed eventual consistency. 

Observed instances of eventual consistency with a number of operations during
tests around 17 December 2014 with a 1-byte object size:

| Provider | read after create | read after delete | read after overwrite | list after create | list after delete | number of operations |
| --- | :---: | :---: | :---: | :---: | :---: | :---: |
| Amazon S3 (us-standard)     | * |  * | * | 16 |  * | 100,000 |
| Amazon S3 (us-west)         | - |  1 | 1 |  * |  * | 100,000 |
| Ceph (DreamObjects)         | - |  - | - |  - |  - |   1,000 |
| Google Cloud Storage        | - |  - | - |  2 |  2 |   1,000 |
| Microsoft Azure Storage     | - |  - | - |  - |  - |   1,000 |
| OpenStack Swift (Rackspace) | - | 17 | 3 | 30 | 20 |   1,000 |

Legend:

* - zero observed instances
* * zero observed instances but expect non-zero

## Documented consistency

* [Amazon S3](http://aws.amazon.com/s3/faqs/#What_data_consistency_model_does_Amazon_S3_employ) - buckets in the US Standard region provide eventual consistency.  Buckets in all other regions provide read-after-write consistency for PUTs of new objects and eventual consistency for overwrite PUTs and DELETEs.
* [Ceph](http://ceph.com/papers/weil-rados-pdsw07.pdf) - provides well-defined safety semantics and strong consistency guarantees.
* [Google Cloud Storage](https://cloud.google.com/storage/docs/concepts-techniques#consistency) - provides strong global consistency for all read-after-write, read-after-update, and read-after-delete operations, including both data and metadata.  List operations are eventually consistent.
* [Microsoft Azure Storage](http://azure.microsoft.com/blog/2014/09/08/managing-concurrency-in-microsoft-azure-storage-2/) - was designed to embrace a strong consistency model which guarantees that when the Storage service commits a data insert or update operation all further accesses to that data will see the latest update.
* [OpenStack Swift](http://docs.openstack.org/developer/swift/overview_architecture.html#updaters) - For example, suppose a container server is under load and a new object is put in to the system. The object will be immediately available for reads as soon as the proxy server responds to the client with success. However, the container server did not update the object listing, and so the update would be queued for a later update. Container listings, therefore, may not immediately contain the object.  [Additional reference](http://lists.openstack.org/pipermail/openstack-dev/2014-June/038881.html).

## References

* [Apache jclouds](https://jclouds.apache.org/) - provides object storage support
* [A Middleware Guaranteeing Client-Centric Consistency on Top of Eventually
Consistent Datastores](http://www.aifb.kit.edu/images/4/44/Ic2e2013consistency.pdf)
* [Benchmarking Eventual Consistency: Lessons Learned from Long-Term Experimental Studies](http://www.aifb.kit.edu/images/8/8d/Ic2e2014.pdf)
* [Eventual Consistency: How soon is eventual?](http://www.researchgate.net/publication/259541556_Eventual_Consistency_How_soon_is_eventual_An_Evaluation_of_Amazon_S3%27s_Consistency_Behavior/links/0deec52c6e04b49921000000)
* [Probabilistically Bounded Staleness](http://pbs.cs.berkeley.edu/) - includes discussion of Riak and Cassandra
