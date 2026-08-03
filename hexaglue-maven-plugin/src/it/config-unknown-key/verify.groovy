def log = new File(basedir, 'build.log').text

assert log.contains('failOnUnclassifed') : 'the mistyped key should be named: ' + log
assert log.contains('known keys are') : 'the keys that are read should be listed: ' + log
return true
