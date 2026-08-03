def log = new File(basedir, 'build.log').text

assert log.contains('[INFERRED]') : 'the refusal should name the gate it failed: ' + log
return true
