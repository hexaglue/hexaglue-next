def log = new File(basedir, 'build.log').text

assert log.contains('1 AGGREGATE_ROOT') : 'the declared kind should be reported: ' + log
assert log.contains('1 declared') : 'a declared kind is not an inferred one: ' + log
return true
