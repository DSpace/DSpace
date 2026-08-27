require "uri"
require "net/http"
require 'json'
require 'pry'
#require 'pry-byebug'
# to connect to the database
# This is a good page with how to use it:  https://zetcode.com/db/postgresqlruby/
#require 'pg'

MCurl            = ENV['MCurl']
MCauthorization  = ENV['MCauthorization']
MCclient_id      = ENV['MCclient_id']
BASE_DATA_DIR    = ENV['BASE_DATA_DIR']

def get_token
#binding.pry

    url = URI("#{MCurl}oauth2/token?grant_type=client_credentials&scope=mcommunity")

#puts "url=" + url.to_s + "\n";

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Post.new(url)
    request["Accept"] = "application/json"
    request["Authorization"] = "Basic " + MCauthorization
    request["x-ibm-client-id"] = MCclient_id
#    request["Cookie"] = MCcookie

    response = https.request(request)
    value = response.read_body
    obj = JSON.parse(value)
    return obj['access_token']
end


def make_request ( unique_id )
    token = get_token

    url = URI("#{MCurl}inst/oauth2/token?grant_type=client_credentials&scope=mcommunity")
    url = URI("#{MCurl}MCommunity/People/" + unique_id)

    https = Net::HTTP.new(url.host, url.port)
    https.use_ssl = true

    request = Net::HTTP::Get.new(url)
    request["Accept"] = "application/json"
    request["Authorization"] = "Bearer " + token
    request["x-ibm-client-id"] = MCclient_id
#    request["Cookie"] = MCcookie

    response = https.request(request)

    value = response.read_body
    return JSON.parse(value)
  end

#print "Hello, World!\n"


#response = make_request ( "davidrea" )
#binding.pry
#affiliation = response["person"]["affiliation"].join(" ") if response["person"]["affiliation"].kind_of?(Array)
#affiliation = response["person"]["affiliation"] if response["person"]["affiliation"].kind_of?(String)
#binding.pry
#affiliation = "user not found" if  response["person"]["affiliation"].nil?
# if there is a proble it is going to be nil.
#binding.pry
#print "response=" + affiliation + "\n";

user_file = BASE_DATA_DIR + "/users.txt"
File.open(user_file, "r") do |f|
  f.each_line do |line|
    puts "testing : " + line

#binding.pry
response = make_request ( line.strip )
#puts response
#binding.pry
affiliation = response["person"]["affiliation"].join(" ") if response["person"]["affiliation"].kind_of?(Array)
affiliation = response["person"]["affiliation"] if response["person"]["affiliation"].kind_of?(String)
#binding.pry
affiliation = "user not found" if  response["person"]["affiliation"].nil?
# if there is a proble it is going to be nil.
#binding.pry



print "user= " + line.strip + "  " + "response=" + affiliation + "\n";

sleep 1.0

  end
end

