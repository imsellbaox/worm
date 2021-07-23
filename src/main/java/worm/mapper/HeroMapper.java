package worm.mapper;

import org.springframework.stereotype.Repository;

import worm.entity.Hero;

@Repository
public interface HeroMapper {
  public void add(Hero hero);
}
