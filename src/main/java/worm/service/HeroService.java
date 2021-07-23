package worm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import worm.entity.Hero;
import worm.mapper.HeroMapper;

@Component
public class HeroService{
	@Autowired(required=true)
	HeroMapper heromapper;
	public void add(Hero hero) {
		heromapper.add(hero);
		System.out.println("++++++++++++++++++++++++++++++++++++++");
	}
	
}
