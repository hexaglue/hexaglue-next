def log = new File(basedir, 'build.log').text

assert log.contains('validation passed') : 'the build disarmed the gate the document armed: ' + log
return true
