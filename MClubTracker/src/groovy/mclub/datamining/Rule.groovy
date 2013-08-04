/**
 * Project: MyForesterApp
 * 
 * File Created at 2013-8-1
 * $id$
 * 
 * Copyright 2013, Shawn Chain (shawn.chain@gmail.com).
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mclub.datamining
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import java.util.concurrent.ConcurrentHashMap

import mclub.util.DateUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author shawn
 *
 */
public interface Rule{
	/**
	 * the rule name
	 * @return
	 */
	public String getName();
	/**
	 *
	 * @param params
	 * @return
	 */
	public abstract int execute(Map<Object,Object> params);
}

public abstract class AbstractRule implements Rule{
	Logger log = LoggerFactory.getLogger(getClass());
	public static final String lastExcutionTimeStamp = "LastExecutionTimeStamp";
	public static final String deviceId = "353451048729261";

	/**
	 * Save rule sate in database
	 * @param stateValue
	 */
	void saveState(Map<String,Object> stateValue){
		// serialize
		def builder = new JsonBuilder()
		builder(stateValue)
		def json = builder.toString()

		RuleState state = RuleState.findByRuleName(this.getName());
		if(!state){
			state = new RuleState()
			state.ruleName = this.getName();
		}
		state.value = json;
		if(!state.save(flush:true)){
			log.error("Error saving rule state:${state.errors}")
		}
	}
	
	/**
	 * Load rules from database
	 * @return
	 */
	Map<String,Object> loadState(){
		def yesterday = DateUtils.yesterday();
		def tomorrow = DateUtils.tomorrow();
		log.info("yest:${yesterday}, tomo:${tomorrow}");
		
		/*
		RuleState state = RuleState.find(
			"FROM RuleState rs WHERE rs.ruleName=:rn AND rs.lastUpdated > :yesterday AND rs.lastUpdated < :tomorrow",
			[rn:this.getName(),yesterday:DateUtils.yesterday(),tomorrow:DateUtils.tomorrow()]
			);
		*/
		RuleState state = RuleState.findByRuleName(getName());
		
		if(!state){
			return [:];
		}
		
		if(state){
			String json = state.value;
			// deserialize
			def slurper = new JsonSlurper()
			try{
				def m = slurper.parseText(json);
				if(m){
					return m;
				}
			}catch(Exception e){
				log.error("Error loading rule state: ${e.message}");
			}
		}
		return [:];
	}

	//TODO - supports multipal devices
	def stateMap;
	public void load(String deviceId){
		stateMap = new HashMap(loadState());
	}
	public void update(String key, Object value){
		if(!stateMap){
			load(null);
		}
		stateMap[key] = value;
	}
	public Map<String,Object> commit(){
		if(stateMap){
			saveState(stateMap);
			stateMap = null;
		}
	}
}
