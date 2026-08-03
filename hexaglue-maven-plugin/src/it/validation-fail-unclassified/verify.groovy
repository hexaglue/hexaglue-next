def log = new File(basedir, 'build.log').text

assert log.contains('validation refused 1 type') : 'the refusal should be counted: ' + log
assert log.contains('com.example.Thing') : 'the refusal should name the type: ' + log
assert log.contains('[UNCLASSIFIED]') : 'the refusal should name the gate it failed: ' + log
assert log.contains('to make it explicit') : 'the refusal should carry its remediation: ' + log
return true
