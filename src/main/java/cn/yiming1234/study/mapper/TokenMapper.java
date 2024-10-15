package cn.yiming1234.study.mapper;

import cn.yiming1234.study.entity.Token;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TokenMapper {

    @Insert("INSERT INTO token (token, time) VALUES (#{token}, #{time})")
    void insertToken(@Param("token") String token, @Param("time") String time);

    @Select("SELECT * FROM token ORDER BY time DESC LIMIT 1")
    Token selectLatestToken();

    @Select("SELECT * FROM token")
    List<Token> selectAllTokens();
}