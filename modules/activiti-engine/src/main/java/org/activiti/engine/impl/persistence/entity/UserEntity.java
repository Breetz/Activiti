/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.db.HasRevision;
import org.activiti.engine.impl.db.PersistentObject;


/**
 * @author Tom Baeyens
 * @author Arkadiy Gornovoy
 */
public class UserEntity implements User, Serializable, PersistentObject, HasRevision {

  private static final long serialVersionUID = 1L;

  protected String id;
  protected int revision;
  protected String firstName;
  protected String lastName;
  protected String email;
  protected String password;
  
  protected final ByteArrayRef pictureByteArrayRef = new ByteArrayRef();
  
  public UserEntity() {
  }
  
  public UserEntity(String id) {
    this.id = id;
  }
  
  public void delete() {
    Context.getCommandContext()
      .getDbSqlSession()
      .delete(this);

    deletePicture();
  }
  
  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<String, Object>();
    persistentState.put("firstName", firstName);
    persistentState.put("lastName", lastName);
    persistentState.put("email", email);
    persistentState.put("password", password);
    persistentState.put("pictureByteArrayId", pictureByteArrayRef.getId());
    return persistentState;
  }
  
  @Override
  public int getRevisionNext() {
    return revision+1;
  }
  
  public Picture getPicture() {
    if(pictureByteArrayRef.getId() != null) {
      return new Picture(pictureByteArrayRef.getBytes(), pictureByteArrayRef.getName());
    }
    return null;
  }
  
  public void setPicture(Picture picture) {
    if(picture != null) {
      savePicture(picture);
    } else {
      deletePicture();
    }      
  }

  protected void savePicture(Picture picture) {
    pictureByteArrayRef.setValue(picture.getMimeType(), picture.getBytes());
  }
  
  protected void deletePicture() {
    pictureByteArrayRef.delete();
  }

  @Override
  public String getId() {
    return id;
  }
  @Override
  public void setId(String id) {
    this.id = id;
  }
  @Override
  public String getFirstName() {
    return firstName;
  }
  @Override
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }
  @Override
  public String getLastName() {
    return lastName;
  }
  @Override
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
  @Override
  public String getEmail() {
    return email;
  }
  @Override
  public void setEmail(String email) {
    this.email = email;
  }
  @Override
  public String getPassword() {
    return password;
  }
  @Override
  public void setPassword(String password) {
    this.password = password;
  }
  @Override
  public int getRevision() {
    return revision;
  }
  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }
  
  @Override
  public boolean isPictureSet() {
    return pictureByteArrayRef.getId() != null;
  }

}
