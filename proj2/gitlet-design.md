# Gitlet Design Document

**Anh Ha**:

## Classes and Data Structures

### Main
This is the entry point to our program. It takes in arguments from the command line and based on 
the command (the first element of the args array) calls the corresponding command in 
Repository which will actually execute the logic of the command. It also validates the 
arguments based on the command to ensure that enough arguments were passed in.

#### Fields
This class has no fields and hence no associated state: it simply validates arguments and defers 
the execution to the Repository class.

### Commit
This class represents a commit that will be stored in a file. 
Commit save in .gitlet/objects/ folder, same real git, two prefix character of commit sha-1 is folder, remain sha-1 character of 
commit is store commit file. 

#### Fields

1. String message: message of this commit
2. Date timestamp: timestamp of this commit
3. String firstParentId: sha-1 id of first parent commit 
4. String secondParentId: sha-1 id of second parent commit, use in case merge feature
5. TreeMap<String, String> fileNameToBlob: Map from file name to blob sha-1 id of files

### StagingArea
This class represents a StagingArea that will be stored in a file. StagingArea contain add files and remove files 
and save in .gitlet/index file 

#### Fields

1. TreeMap<String, String> addFiles: file name and blob sha-1 id of file 
2. TreeSet<String> removeFiles: list remove file name 



### Head

### 


## Algorithms

## Persistence

