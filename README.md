# :cloud_with_lightning: lighty YANG Validator 15.3.0

This tool validates YANG modules using the YANG-Tools parser. If there are any problems parsing the module, it will show the stacktrace with the problem, linking to the corresponding module.

> This makes lighty YANG Validator **a must-have tool, when using [OpenDaylight](https://github.com/opendaylight) or [lighty.io](https://github.com/PANTHEONtech/lighty).**

[YANG Tools](https://github.com/opendaylight/yangtools) help parse YANG modules, represent the YANG model in Java and serialize / deserialize YANG model data. However, custom YANG module can contain improper data, which would result in an application failure. To avoid these situations, [PANTHEON.tech](https://pantheon.tech) engineers created the lighty YANG Validator.

## Compile, Build & Generate Distribution
Go to the root directory `/lighty-yang-validator/` and use the command:

```
mvn clean install
```

The distribution will be stored in the **"target"** directory, as a file called *lighty-yang-validator-15.3.0-SNAPSHOT-bin.zip*

## Run from Distribution

1. Unzip the distribution:

```
unzip lighty-yang-validator-15.3.0-SNAPSHOT-bin.zip
```

2. Enter the directory, to which the distribution was extracted to.

3. Run the script with the command:

```
./lyv \<options>
```

To parse the module, we need to: 
- [x] have all the imported and included modules of a testing module on the same path 
- [x] or we need to add a `-p` or `--path` option (with a path or column separated paths to the modules that are needed) 

Use `-r` or `--recursive` option to search for the files recursively, within the file structure.

## Options

* **Logs**: Use the `-o` or `--output` option, to specify the path for the output file directory for logs.

* **Parse All**: Use the `-a` or `--parse-all` option to parse all files within given directory. This option can be used with the `-p` option.

* **Search**: Use `-p` or `--path` option, to specify path as a colon (:) separated list of directories, to search for YANG modules.

* **Recursive Search**: Use `-r` or `--recursive` option, to specify recursive search of directories specified by `-p` or `--path` option.

* **Search by Module Name**: Use `-m` or `--module-name` option, to search for file by module name instead of specifying the whole path.

* **Prune and Search**: Use `-e` or `--features` option, to prune the data model by removing all nodes that are defined with a *if-feature*.

* **Print Help Message**: Use `-h` or `--help` option, to print help message and exit.

* **Specify Output Format**: Use **-f, --format** option to specify output format. Supported formats: 
  * tree
  * depend
  * yang
  * json-tree
  * jstree
  * name-revision.

* **Simplify YANG**: Use `-s` or `--simplify` option, to to simplify the YANG file. The YANG file will be simplified,
  based on the nodes used in the XML file. Use with `-o` to specify output directory where will be simplified yang generated.
  In this case out.log file will contain only error message if some error will be present. Without specified output directory
  will be result printed to *stdout*.

## Formats

* tree: tree is printed in following format *\<status>--\<flags> \<name>\<opts> \<type> <if-features>*

 \<status> is either:

    +  for current
    x  for deprecated
    o  for obsolete

 \<flags> is either:

    rw  for configuration data
    ro  for non-configuration data, output parameters to rpcs
       and actions, and notification parameters
    -w  for input parameters to rpcs and actions
    -x  for rpcs and actions
    -n  for notifications

 \<name> is the name of the node:

    (<name>) means that the node is a choice node
    :(<name>) means that the node is a case node

 \<opts> is either:

    ?  for an optional leaf, choice
    *  for a leaf-list or list
    [<keys>] for a list's keys

 \<type> is the name of the type for leafs and leaf-lists.
  If the type is a leafref, the type is printed as "-> TARGET",
  whereTARGET is the leafref path, with prefixes removed if possible.

 \<if-features> is the list of features this node depends on, printed
     within curly brackets and a question mark "{...}?"

* **name-revision**: name-revision is printed as the following format:

```
\<module_name>@\<revision>
```

* **depend**: list of all the modules that the validated module depends on

* **json-tree**: generates a json tree with all the node information

* **jstree**: generates a html with java script with a yang tree

* **yang**: generates a yang file (used with simplify will print
the simplified yang file)

## Examples

* Validate: To **validate the module only**:

```
./lyv \<path_to_the_yang_module>
```

* **Validate w/ Dependencies**: To validate a module, which has **dependencies on a different path recursively**:

```
./lyv -r -p \<path_to_module_dependencies>
\<path_to_the_yang_module>
```

* **Format YANG Tree**: To create a **formatted YANG tree**:

```
/lyv -f tree \<path_to_the_yang_module>
```

* **YANG jstree**: To create **formatted YANG jstree**:

```
./lyv -f jstree \<path_to_the_yang_module>
```

* **Simplify YANG**: To simplify and print the YANG file, **based on XML**:

```
./lyv -o \<path_to_output_directory> -s
\<path_to_xml_files> -p \<path_to_yang_modules> -f yang
```

# References

The lighty YANG Validator release corresponds with the latest release of [lighty.io](https://github.com/PANTHEONtech/lighty). Learn more [about lighty.io here](https://lighty.io).

Inspiration from this tool comes from the [pyang YANG Validator](https://github.com/mbj4668/pyang).
