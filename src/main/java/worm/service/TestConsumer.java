package worm.service;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import worm.entity.Hero;

@Component
public class TestConsumer {
	public static Logger log =LoggerFactory.getLogger(TestConsumer.class);
	
	@Autowired 
	KafkaTemplate<String, String> kafkaTemplate;
	
	@Autowired 
	HeroService heroservice;
	
	HashSet<String> secretcode =new HashSet<>();
	
	HashSet<String> urlcode = new HashSet<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean("ackContainerFactory")
	public ConcurrentKafkaListenerContainerFactory ackContainerFactory(ConsumerFactory consumerFactory) {
		ConcurrentKafkaListenerContainerFactory factory = new ConcurrentKafkaListenerContainerFactory();
		factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
		factory.setConsumerFactory(consumerFactory);
		return factory;
	}
	
	@KafkaListener(topics = "wormhero", containerFactory = "ackContainerFactory")
	public  void worm(ConsumerRecord<String, String> cr, Acknowledgment ack) throws IOException {
		String url =cr.value();
		log.warn("url:"+url+",offset:"+cr.offset());
		//非网页过滤
		if(isBlank(url)){
			return;
		}
		String doGet =doGet(url);
		Document doc =Jsoup.parse(doGet);
		//爬取url
		Elements sonsurl =doc.getElementsByTag("a");
		Iterator<Element> urliter = sonsurl.iterator();
		while(urliter.hasNext()){
			Element purl =urliter.next();
			String href =purl.attr("href");
			if(href.contains("http")&& !href.contains("javascript")){
//				urlcode.add(href);
//				kafkaTemplate.send("wormhero",href);
				if(urlcode.contains(href)){
					return;
				}else{
					urlcode.add(href);
					kafkaTemplate.send("wormhero",href);					
				}
				log.error("个数"+urlcode.size());
			}
		}
		//爬取名字：
		Elements heroNameTag =doc.getElementsByClass("hero_parameter_tit");
		//若为非目标网页，则直接返回
		if(heroNameTag==null){
			ack.acknowledge();
			log.error("kafka unable to submit,offset:"+cr.offset());
			return ;
		}
/*
		Document parse1 =Jsoup.parse(heroNameTag.html());
		Elements heroName =parse1.getElementsByTag("h1");
//		String name =heroName.iterator().next().text();
		String name =heroName.get(0).text();
		
		if(secretcode.contains(stringMD5(name))){
			return ;
		}else{
			secretcode.add(stringMD5(name));
		}
		//爬取技能
		Elements heroSkillTag=doc.getElementsByClass("content_li anchor_skillflow");
		Document parse2 =Jsoup.parse(heroSkillTag.html());
		Elements heroSkill =parse2.getElementsByTag("h6");
//		Hero h =new Hero();
//		h.setName(name);
//		h.setPassive(heroSkill.get(0).text());
//		h.setQ(heroSkill.get(1).text());
//		h.setW(heroSkill.get(2).text());
//		h.setE(heroSkill.get(3).text());
//		h.setR(heroSkill.get(4).text());
//		heroservice.add(h);*/
		ack.acknowledge();
		
	}
	
	public String stringMD5(String name){
		  byte[] secret =null;
		  try {
			secret = MessageDigest.getInstance("md5").digest(
					name.getBytes());
		} catch (Exception e) {
			throw new RuntimeException("没有这个md5算法");
		}
		  String md5code =new BigInteger(1,secret).toString(16);
		  for(int i=0;i<32-md5code.length();i++){
			  md5code = "0"+md5code;
		  }
		  return md5code;
		}
	
	private static String doGet(String url) throws HttpException, IOException {
		String result=null;
		HttpClient httpClient =new HttpClient();
		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
		GetMethod get =new GetMethod(url);
		get.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 60000);
		get.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(105,true));
		
		int statusCode =httpClient.executeMethod(get);
		long length=get.getResponseContentLength();
		Header header =get.getResponseHeader("Content-Type");
		if(header !=null){
			log.info(header.getName()+","+header.getValue());
			String value =header.getValue();
			if(value.contains("text/asp")||value.contains("text/html")||value.contains("text/plain")){
				
			}else{
				return null;
			}
		}else{
			return null;
		}
		if(length>5242880){
			log.info("request file length:"+length);
			return null;
		}
		if(statusCode !=HttpStatus.SC_OK){
			log.error("method faild:"+get.getStatusLine());
		}else{
			try (InputStream is =get.getResponseBodyAsStream();
					BufferedReader br =new BufferedReader(new InputStreamReader(is,"UTF-8"));
				){
				StringBuffer sbf =new StringBuffer();
				String temp =null;
				while((temp = br.readLine()) !=null){
					sbf.append(temp).append("\r\n");
				}
				result = sbf.toString();
			} catch (Exception e) {
			
			}
		}
		get.releaseConnection();
		return result;
	}

}
