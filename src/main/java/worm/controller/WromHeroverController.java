package worm.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("kafka")
public class WromHeroverController {

	public static Logger log =LoggerFactory.getLogger(WromHeroverController.class);
		
	@Autowired 
	private  KafkaTemplate<String, String> kafkaTemplate;
	
	@RequestMapping("send")
	public String send(){
		kafkaTemplate.send("wormhero","http://cha.17173.com/lol/");
		return "success";
	}
}
