def log = new File(basedir, 'build.log').text

assert log.contains('HexaGlue is skipped') : 'the goal should say it did nothing: ' + log
assert !log.contains('types analysed') : 'nothing should have been analysed: ' + log
return true
