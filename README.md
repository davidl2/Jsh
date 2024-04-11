# Jsh

Simple JDBC shell. Uses the following meta-commands:

- `DRIVER`:  load a JDBC driver class (`Class.forName("...")`).
- `USER`: specify a username for a subsequent OPEN command
- `PASS`: specify a password for a subsequent OPEN command
- `OPEN`: connect to a database (JDBC URL)
- `TRACE`: print out the stack-trace from the last failed command
- `QUIT` or `\q`: exit the shell

Anything else is taken as an SQL Query: passed to the database, executed,
and the result(s) displayed.
