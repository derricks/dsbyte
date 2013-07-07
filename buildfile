require 'rubygems'

repositories.remote << 'https://oss.sonatype.org/content/groups/scala-tools/'
require 'buildr/scala'

define 'dsbyte' do
  test.using(:scalatest)
end