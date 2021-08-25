# StreamBlock graalvm

## Usage
Set JAVA_HOME to graalvm 21.2.0 version root and add graalvm-21.2.0/bin subfolder to PATH.
```
mvn package
./cal <path> --cal.actor=<actor to run> --cal.iterations=<number of iterations> --cal.directory-lookup=<true to recursively load and parse all the files in the directory>
```
### Example Usage
To execute the test program ```language/tests/println.cal```:
```bash
./cal language/tests/println.cal --cal.actor=test.test --cal.iterations=1
```
## Project Status

### Namespace
 - [x] Multiple files
 - [x] Named imports
 - [ ] Import all, currently we are parsing all the files but this need to be done with a DAG in order to have the entities to import with all
 - [ ] Import functions and global variables

### Actions
 - [x] Basic support
 - [x] Simple, single-port input pattern
 - [X] repeat, lacking proper multiport support
 - [ ] Other input patterns
 - [x] Simple, single-port output expressions
 - [ ] Delays
 - [x] Guards
 - [ ] Priorities

### Satements/Expressions
 - [x] Assignments
 - [x] Calls
 - [x] Lambdas
 - [x] most literals
 - [x] if
 - [x] for, doesn't support multiple var decls and probably othe little things, comprehensions lack filter support
 - [ ] while
 - [ ] choose
 - [x] +, -, \*, /,
 - [ ] # (list size)

### Types
 - [x] Numbers, all implemented as BigNumbers
 - [x] List, room for improvements does not subtype Seq and Collection
 - [x] Strings, does not subtype List[Character] yet
 - [ ] Seq
 - [ ] Collection
 - [ ] Map
 - [ ] Set
 - [ ] Character
 - [ ] Boolean
 - [ ] Null
 - [ ] Custom types

### Actor
 - [x] Simple execution
 - [ ] FSM-action selection

### Network
 - [x] Simple execution

